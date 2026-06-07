# Zeta Server

Java 8 + Spring Boot 2.7 + JPA + MySQL + Redis 后端服务。

## 技术栈

- Spring Boot 2.7.18
- Spring Data JPA + MySQL
- Spring Data Redis（登录 Token 存储）
- REST API

## 环境配置

开发配置按模块拆分，由 `application-dev.yml` 导入：

```
src/main/resources/dev/
├── mysql.yml      # 数据源（复制 mysql.yml.example）
├── redis.yml      # Redis（复制 redis.yml.example）
└── jwt.yml        # JWT 参数（预留，复制 jwt.yml.example）
```

首次配置：

```bash
cd src/main/resources/dev
cp mysql.yml.example mysql.yml
cp redis.yml.example redis.yml
cp jwt.yml.example jwt.yml
# 编辑各文件填入实际连接信息
```

`dev/*.yml` 已加入 `.gitignore`，请勿将账号密码提交到仓库。

## 启动

```bash
mvn spring-boot:run
```

默认 `spring.profiles.active=dev`，服务地址：`http://localhost:8080`

### 知识结构升级（旧库迁移）

若从旧版本升级，启动时报 `protection_logics` 的 `device_id` 外键错误，说明库中已有保护逻辑数据尚未关联设备。可任选其一：

1. **推荐**：执行修复脚本后重启
   ```bash
   mysql -u用户 -p 数据库名 < scripts/fix-protection-logics-fk.sql
   mvn spring-boot:run
   ```
   启动后 `DataInitializer` 会自动将未关联设备的保护逻辑挂到默认设备。

2. **开发环境清空**：删除 `protection_logics`、`devices`、`cabinets` 表后重启，由种子数据重新注入。

## 测试账号

| 用户名 | 密码 | 角色 |
|--------|------|------|
| student | 123456 | STUDENT |
| teacher | 123456 | TEACHER |
| admin | 123456 | ADMIN |

首次启动会自动初始化用户与保护逻辑样本数据。

## 鉴权

采用 **JWT 双 Token**：

| Token | 说明 | 存储 |
|-------|------|------|
| accessToken | 短期访问令牌（JWT） | 前端 localStorage，请求头 `Authorization: Bearer` |
| refreshToken | 长期刷新令牌 | Redis + 前端 localStorage |

| 方法 | 路径 | 说明 |
|------|------|------|
| POST | `/api/auth/login` | 登录，返回 accessToken + refreshToken |
| POST | `/api/auth/refresh` | 刷新令牌（body: `{ refreshToken }`） |
| POST | `/api/auth/logout` | 注销（body: `{ refreshToken }`） |
| GET | `/api/auth/me` | 当前用户（Bearer accessToken） |
| POST | `/api/auth/change-password` | 修改当前用户密码 |
| GET | `/api/users?role=STUDENT/TEACHER/ADMIN` | 按角色查询用户列表（仅 ADMIN，`role` 必填） |
| POST | `/api/users` | 创建用户（仅 ADMIN） |
| GET | `/api/users/{id}` | 用户详情（仅 ADMIN） |
| PUT | `/api/users/{id}` | 更新用户显示名与角色（仅 ADMIN） |
| PUT | `/api/users/{id}/password` | 重置用户密码（仅 ADMIN） |
| DELETE | `/api/users/{id}` | 删除用户（仅 ADMIN，不可删自己） |
| GET | `/api/cabinets` | 屏柜列表（仅 ADMIN，含停用） |
| POST | `/api/cabinets` | 创建屏柜（仅 ADMIN） |
| GET | `/api/cabinets/{id}` | 屏柜详情（仅 ADMIN） |
| PUT | `/api/cabinets/{id}` | 更新屏柜（仅 ADMIN） |
| DELETE | `/api/cabinets/{id}` | 删除屏柜（仅 ADMIN，无下属设备时） |
| GET | `/api/cabinets/{id}/devices` | 屏柜下设备列表（仅 ADMIN） |
| POST | `/api/cabinets/{id}/devices` | 在屏柜下创建设备（仅 ADMIN） |
| GET | `/api/devices/{id}` | 设备详情（仅 ADMIN） |
| PUT | `/api/devices/{id}` | 更新设备（仅 ADMIN） |
| DELETE | `/api/devices/{id}` | 删除设备（仅 ADMIN，无下属保护逻辑时） |
| GET | `/api/devices/{id}/protection-logics` | 设备下保护逻辑列表（仅 ADMIN） |
| POST | `/api/devices/{id}/protection-logics` | 在设备下创建保护逻辑（仅 ADMIN） |
| PUT | `/api/admin/protection-logics/{id}` | 更新保护逻辑（仅 ADMIN） |
| DELETE | `/api/admin/protection-logics/{id}` | 删除保护逻辑（仅 ADMIN） |
| GET | `/api/devices/{id}/cognition-items` | 设备认知条目列表（仅 ADMIN） |
| POST | `/api/devices/{id}/cognition-items` | 创建设备认知条目（仅 ADMIN） |
| PUT | `/api/admin/device-cognition-items/{id}` | 更新设备认知条目（仅 ADMIN） |
| PUT | `/api/admin/protection-logics/{id}/config` | 更新保护逻辑 JSON 配置（仅 ADMIN） |
| GET | `/api/admin/protection-logics/{id}/config` | 获取保护逻辑 JSON 配置（仅 ADMIN） |
| DELETE | `/api/admin/device-cognition-items/{id}` | 删除设备认知条目（仅 ADMIN） |
| GET | `/api/knowledge/tree` | 知识结构树（屏柜 → 设备 → 保护逻辑） |
| GET | `/api/knowledge/cabinets` | 屏柜列表 |
| GET | `/api/knowledge/cabinets/{id}` | 屏柜详情（含设备） |
| GET | `/api/knowledge/cabinets/{id}/devices` | 屏柜下设备列表 |
| GET | `/api/knowledge/devices/{id}` | 设备详情（含保护逻辑） |
| GET | `/api/knowledge/devices/{id}/protection-logics` | 设备下保护逻辑列表 |
| GET | `/api/protection-logics` | 保护逻辑列表（扁平，兼容学员全景） |
| GET | `/api/protection-logics/{id}` | 保护逻辑详情 |
| GET | `/api/protection-logics/{id}/sections` | 实验断面列表 |

JWT 参数见 `dev/jwt.yml`（`jwt.expiration.access` / `jwt.expiration.refresh`，单位秒）。
