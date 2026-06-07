# Zeta 业务系统

继电保护逻辑**业务系统** — 学员/教师/管理员上层业务与界面展示。

与**屏柜系统**分工：

| 系统 | 职责 | 数据库 |
|------|------|--------|
| **屏柜系统**（独立工程） | 屏柜结构、信号逻辑、录播、设备数值召回等硬件相关 | `ct-screen`（本系统只读） |
| **本系统（业务）** | 用户、教学流程、展示介绍、业务流程 | `ct-screen-monitor`（读写） |

两系统通过 **Redis 队列** 异步交互。

## 目录

| 目录 | 说明 | 技术栈 |
|------|------|--------|
| `admin/` | 管理后台（含学员/教师/管理员界面） | Vite + React |
| `front/` | 前台客户端（待开发） | Electron + Vite + React |
| `server/` | 业务 API 服务 | Java 8 + Spring Boot 2.7 |
| `packages/diagram/` | 共享框图渲染（V4 / ELK + React Flow） | TypeScript |

## 后端模块划分（`server`）

按实体分包，详见 `server/README.md`：

```
com.zeta.business.user / auth / devicedisplay / cabinetdisplay
com.zeta.screen.cabinet / ieddevice / logicdiagram / knowledge
com.zeta.integration.queue.*
```

## 数据库

- **ct-screen-monitor**：JPA `ddl-auto: update`，业务侧维护表结构
- **ct-screen**：`ddl-auto: none`，只读连接；表结构见项目根 `schema.sql`
- 方言：`MariaDB103Dialect`（兼容 MySQL 8 开发 / MariaDB 10.5 生产）
- 表结构见项目根 [`schema-monitor.sql`](../schema-monitor.sql)（屏柜库见 [`schema.sql`](../schema.sql)）

配置分层见 `server/README.md`：`application-dev.yml` 管行为开关，`dev/mysql.yml` 管双库连接。

## 快速启动

```bash
# 后端
cd server && mvn spring-boot:run

# 管理端
cd admin && npm install && npm run dev
```

## 测试账号

| 用户名 | 密码 | 角色 |
|--------|------|------|
| student | 123456 | STUDENT |
| teacher | 123456 | TEACHER |
| admin | 123456 | ADMIN |
