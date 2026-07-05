# 静态文件数据库存储迁移方案

## 背景

**需求**：甲方要求静态数据（图片、视频）存入数据库，不额外存储静态文件

**现状**：
- 图片存储在服务器文件系统 `./data/uploads/` 目录
- 数据库仅存储文件路径（`image_url` 字段）
- 通过 `WebConfig.addResourceHandler("/uploads/**")` 提供静态文件访问

**目标**：
- 图片二进制数据直接存入数据库 BLOB 字段
- 通过 API 端点访问图片，移除文件系统依赖

---

## 实施步骤

### 阶段 1：数据库 Schema 变更

#### 1.1 备份数据库

```bash
mysqldump -u root -p ct-screen-monitor > backup_$(date +%Y%m%d_%H%M%S).sql
```

#### 1.2 执行 Schema 变更

**文件**: `schema_migration.sql`

```sql
-- cabinet_display_item 表
ALTER TABLE cabinet_display_item 
  ADD COLUMN image_data LONGBLOB COMMENT '图片二进制数据',
  ADD COLUMN image_content_type VARCHAR(100) COMMENT 'MIME类型',
  MODIFY COLUMN image_url VARCHAR(512) NULL COMMENT '兼容旧数据';

-- device_display_item 表
ALTER TABLE device_display_item
  ADD COLUMN image_data LONGBLOB COMMENT '图片二进制数据',
  ADD COLUMN image_content_type VARCHAR(100) COMMENT 'MIME类型',
  MODIFY COLUMN image_url VARCHAR(512) NULL COMMENT '兼容旧数据';
```

**执行**：
```bash
mysql -u root -p ct-screen-monitor < schema_migration.sql
```

---

### 阶段 2：后端代码改造

#### 2.1 实体层添加字段

**文件**: `CabinetDisplayItem.java`, `DeviceDisplayItem.java`

添加字段：
```java
@Lob
@Column(name = "image_data")
private byte[] imageData;

@Column(name = "image_content_type", length = 100)
private String imageContentType;
```

#### 2.2 改造存储服务

**文件**: `CabinetDisplayImageStorage.java`

主要变更：
- 新增 `storeToDatabase()` 方法：保存文件到数据库，返回记录ID
- 新增 `loadImage()` 方法：从数据库读取图片
- 保留旧方法用于向后兼容（可选）

#### 2.3 新增图片访问 API

**文件**: `ImageController.java` (新建)

端点：
```
GET /api/images/cabinet-display/{id}
GET /api/images/device-display/{id}
```

响应：
- `Content-Type`: 从 `image_content_type` 读取
- `Body`: `image_data` 二进制数据

#### 2.4 修改上传 Controller

**文件**: `CabinetDisplayImageController.java`

变更：
- `upload()` 方法改为调用 `storageService.storeToDatabase()`
- 返回 `imageId` 而非 `imageUrl`

#### 2.5 移除静态文件配置

**文件**: `WebConfig.java`

删除：
```java
.addResourceHandler("/uploads/**")
.addResourceLocations("file:" + uploadProperties.getStorageDir() + "/")
```

---

### 阶段 3：前端改造

#### 3.1 修改 client.js

**文件**: `admin/src/api/client.js`

变更：
```javascript
// 旧版本
export function imageUrl(path) {
  if (!path) return ''
  return BASE + path
}

// 新版本
export function imageUrl(type, id) {
  if (!id) return ''
  return BASE + `/api/images/${type}/${id}`
}
```

#### 3.2 更新所有组件

搜索并替换：
```javascript
// 查找
imageUrl(item.imageUrl)

// 替换为
imageUrl('cabinet-display', item.id)
// 或
imageUrl('device-display', item.id)
```

涉及文件：
- `PlateCognitionContent.jsx`
- `DeviceCognitionContent.jsx`
- `StructureCognitionContent.jsx`
- `TerminalCognitionContent.jsx`
- `CabinetDisplayItemsPage.jsx`
- `DeviceDisplayItemsPage.jsx`
- 其他使用图片的组件

---

### 阶段 4：数据迁移脚本

#### 4.1 创建迁移服务

**文件**: `MigrationService.java` (新建)

```java
@Service
@Slf4j
public class MigrationService {
    
    @Autowired
    private CabinetDisplayItemRepository cabinetRepository;
    
    @Autowired
    private DeviceDisplayItemRepository deviceRepository;
    
    @Autowired
    private UploadProperties uploadProperties;
    
    @PostConstruct
    public void migrateExistingImages() {
        log.info("开始迁移历史图片数据...");
        
        // 迁移 cabinet_display_item
        migrateCabinetImages();
        
        // 迁移 device_display_item
        migrateDeviceImages();
        
        log.info("图片数据迁移完成");
    }
    
    private void migrateCabinetImages() {
        List<CabinetDisplayItem> items = cabinetRepository.findAllWithImageUrl();
        log.info("发现 {} 条 cabinet_display_item 需要迁移", items.size());
        
        for (CabinetDisplayItem item : items) {
            try {
                Path filePath = resolveFilePath(item.getImageUrl());
                if (Files.exists(filePath)) {
                    byte[] data = Files.readAllBytes(filePath);
                    String contentType = Files.probeContentType(filePath);
                    
                    item.setImageData(data);
                    item.setImageContentType(contentType != null ? contentType : "image/jpeg");
                    cabinetRepository.save(item);
                    
                    log.debug("迁移成功: id={}, url={}", item.getId(), item.getImageUrl());
                } else {
                    log.warn("文件不存在: id={}, url={}", item.getId(), item.getImageUrl());
                }
            } catch (Exception e) {
                log.error("迁移失败: id={}, error={}", item.getId(), e.getMessage());
            }
        }
    }
    
    private void migrateDeviceImages() {
        // 同上逻辑
    }
    
    private Path resolveFilePath(String imageUrl) {
        String relative = imageUrl.substring("/uploads/".length());
        return Paths.get(uploadProperties.getStorageDir(), relative);
    }
}
```

#### 4.2 添加 Repository 方法

**文件**: `CabinetDisplayItemRepository.java`

```java
@Query("SELECT c FROM CabinetDisplayItem c WHERE c.imageUrl IS NOT NULL AND c.imageData IS NULL")
List<CabinetDisplayItem> findAllWithImageUrl();
```

**文件**: `DeviceDisplayItemRepository.java` - 同上

---

### 阶段 5：部署与验证

#### 5.1 部署步骤

1. **停止后端服务**
   ```bash
   systemctl stop zeta-backend
   ```

2. **执行数据库迁移**
   ```bash
   mysql -u root -p ct-screen-monitor < schema_migration.sql
   ```

3. **部署新代码**
   - 后端：编译并部署新版本 JAR
   - 前端：构建并部署新版本

4. **启动后端服务**
   ```bash
   systemctl start zeta-backend
   ```

5. **检查迁移日志**
   ```bash
   journalctl -u zeta-backend -f | grep "迁移"
   ```

#### 5.2 验证清单

- [ ] 上传新图片 → 检查数据库 `image_data` 字段是否有数据
- [ ] 访问 `/api/images/cabinet-display/{id}` → 图片正常显示
- [ ] 前端页面图片正常加载
- [ ] 旧图片（迁移前）正常显示
- [ ] 新图片（迁移后）正常显示
- [ ] 检查 `/opt/zeta/data/uploads/` 目录是否还有新文件产生

#### 5.3 清理（可选）

确认所有数据迁移成功且系统运行稳定后：

1. **删除旧文件**
   ```bash
   rm -rf /opt/zeta/data/uploads/
   ```

2. **移除迁移代码**
   - 删除 `MigrationService.java`
   - 删除 Repository 中的 `findAllWithImageUrl()` 方法

3. **重新部署**

---

## 回滚方案

如果迁移失败，可快速回滚：

1. **恢复数据库**
   ```bash
   mysql -u root -p ct-screen-monitor < backup_xxx.sql
   ```

2. **回滚代码**
   - 后端：部署旧版本
   - 前端：部署旧版本

3. **重启服务**

---

## 注意事项

1. **性能考虑**：
   - LONGBLOB 会增加数据库体积，确保数据库服务器有足够磁盘空间
   - 大图片（>5MB）可能影响数据库查询性能，建议压缩后再存储

2. **缓存策略**：
   - 图片 API 应添加 `Cache-Control` 响应头
   - 前端可使用 `useMemo` 缓存图片 URL

3. **事务处理**：
   - 图片上传和记录保存应在同一事务中
   - 迁移脚本应逐条处理，避免大事务

4. **并发控制**：
   - 迁移期间避免用户上传图片
   - 可在迁移前发布维护公告

---

## 实施记录

| 步骤 | 状态 | 时间 | 备注 |
|---|---|---|---|
| 数据库备份 | ⏳ | - | - |
| Schema 变更 | ⏳ | - | - |
| 后端代码改造 | ⏳ | - | - |
| 前端改造 | ⏳ | - | - |
| 数据迁移 | ⏳ | - | - |
| 验证测试 | ⏳ | - | - |
| 清理旧文件 | ⏳ | - | - |

---

**创建时间**: 2026-07-03  
**创建人**: AI Assistant  
**版本**: v1.0
