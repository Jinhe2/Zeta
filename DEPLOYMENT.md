# 交付部署指南

## 项目概述

继电保护逻辑教学系统，包含前端（React）和后端（Spring Boot），支持本地局域网部署。

## 交付内容

### 前端
- **技术栈**: React 19 + Vite
- **构建产物**: `code/admin/dist/` 目录
- **部署方式**: 静态文件，Nginx 托管

### 后端
- **技术栈**: Java 8 + Spring Boot 2.7
- **构建产物**: `code/server/target/zeta-server-1.0.0.jar`
- **依赖**: MySQL 8.0+, Redis 6.0+

## 部署步骤

### 1. 后端部署

```bash
# 构建
cd code/server
mvn clean package -DskipTests

# 运行
java -jar target/zeta-server-1.0.0.jar \
  --spring.profiles.active=prod \
  --server.port=8080

# 或使用 systemd 管理（推荐）
sudo systemctl start zeta-server
```

**配置文件** (`application-prod.yml`):
- 数据库连接: MySQL 地址、用户名、密码
- Redis 连接: Redis 地址、端口、密码
- 文件存储: 图片上传目录路径

### 2. 前端部署

```bash
# 构建
cd code/admin
npm install
npm run build

# 部署到 Nginx
sudo cp -r dist/* /var/www/zeta/

# 重载 Nginx
sudo nginx -s reload
```

**Nginx 配置** (`/etc/nginx/sites-available/zeta`):
```nginx
server {
    listen 80;
    server_name _;

    # 前端静态文件
    location / {
        root /var/www/zeta;
        try_files $uri $uri/ /index.html;
    }

    # 后端 API 代理
    location /api/ {
        proxy_pass http://localhost:8080;
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
    }

    # 图片访问
    location /api/images/ {
        proxy_pass http://localhost:8080;
    }
}
```

### 3. 数据库初始化

```bash
# 创建数据库
mysql -u root -p -e "CREATE DATABASE zeta_business;"
mysql -u root -p -e "CREATE DATABASE zeta_screen;"

# 导入 Schema
mysql -u root -p zeta_business < schema.sql
mysql -u root -p zeta_screen < schema.sql

# 执行迁移脚本（如从旧版本升级）
mysql -u root -p zeta_business < db/migration/V2__add_image_data_columns.sql
```

## 客户端配置（甲方操作）

### 首次使用

1. 浏览器访问系统（Nginx 地址）
2. 在登录页点击右上角 **⚙** 按钮
3. 输入后端服务器地址（格式：`IP:端口`，如 `192.168.1.100:8080`）
4. 点击"测试连接"验证
5. 点击"保存"
6. 使用账号密码登录

### 默认账号

| 角色 | 用户名 | 密码 |
|------|--------|------|
| 学员 | student | 123456 |
| 教师 | teacher | 123456 |
| 管理员 | admin | 123456 |

**⚠️ 生产环境请立即修改默认密码**

### 修改服务器地址

1. 登出系统
2. 在登录页重新配置服务器地址

## 图片存储迁移

系统支持将图片存储在数据库中（而非文件系统），便于备份和迁移。

### 自动迁移

首次启动后端时，`ImageMigrationService` 会自动将旧文件系统中的图片迁移到数据库。

**检查迁移状态**:
```bash
# 查看日志
journalctl -u zeta-server | grep "图片数据迁移"

# 检查数据库
mysql -u root -p zeta_business -e "
SELECT COUNT(*) as total,
       SUM(CASE WHEN image_data IS NOT NULL THEN 1 ELSE 0 END) as migrated
FROM cabinet_display_items;
"
```

### 清理旧文件

确认迁移成功后，可删除旧文件目录：
```bash
sudo rm -rf /opt/zeta/data/uploads/
```

## 常见问题

### Q: 登录后提示"请先配置服务器地址"

**A**: 点击右上角 ⚙ 按钮，配置后端服务器地址并保存。

### Q: 图片无法显示

**A**: 
1. 检查后端 `/api/images/` 接口是否正常
2. 检查数据库中 `image_data` 字段是否有数据
3. 查看后端日志是否有错误

### Q: 连接测试失败

**A**: 
1. 确认后端服务已启动
2. 确认 IP 和端口正确
3. 检查防火墙是否放行端口
4. 尝试用浏览器直接访问 `http://IP:端口/api/auth/me`

### Q: 如何备份数据

**A**: 
```bash
# 备份业务数据库
mysqldump -u root -p zeta_business > backup_$(date +%Y%m%d).sql

# 备份配置数据库
mysqldump -u root -p zeta_screen > screen_backup_$(date +%Y%m%d).sql
```

## 版本信息

- **前端**: 1.0.0
- **后端**: 1.0.0
- **数据库**: V2 (含图片数据库存储)

## 技术支持

如遇问题，请提供：
1. 错误截图
2. 后端日志 (`journalctl -u zeta-server -n 100`)
3. 浏览器控制台错误信息
4. 服务器地址配置

---

**文档版本**: 1.0  
**更新日期**: 2026-07-03
