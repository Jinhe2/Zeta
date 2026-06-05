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
| GET | `/api/protection-logics` | 保护逻辑列表 |
| GET | `/api/protection-logics/{id}` | 保护逻辑详情 |
| GET | `/api/protection-logics/{id}/sections` | 实验断面列表 |

JWT 参数见 `dev/jwt.yml`（`jwt.expiration.access` / `jwt.expiration.refresh`，单位秒）。
