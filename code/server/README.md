# Zeta Server（业务系统）

Java 8 + Spring Boot 2.7 业务 API。连接 **ct-screen-monitor**（读写）与 **ct-screen**（只读），经 Redis 队列与屏柜系统交互。

## 代码组织（按实体分包）

每个实体目录包含 **Entity → Repository → Service → Controller** 及该实体相关的请求/响应 DTO。

```
com.zeta.business/
├── user/              用户
├── auth/              登录鉴权、JWT
├── devicedisplay/     设备展示条目
└── cabinetdisplay/    屏柜展示条目

com.zeta.screen/
├── cabinet/           屏柜（只读）
├── ieddevice/         IED 装置（只读）
├── logicdiagram/      保护逻辑框图（只读）
└── knowledge/         知识结构聚合 API

com.zeta.config/       数据源、JPA、Redis 配置
com.zeta.web/          全局异常、工具类
com.zeta.integration.queue/  Redis 队列
```

## 系统边界

| 模块包 | 数据库 | 说明 |
|--------|--------|------|
| `com.zeta.business.*` | ct-screen-monitor | 用户、屏柜/设备展示、业务流程 |
| `com.zeta.screen.*` | ct-screen（只读） | 屏柜/设备/保护逻辑查询与学员展示 |
| `com.zeta.integration.queue.*` | Redis | 屏柜系统 ↔ 业务系统消息队列 |

屏柜系统负责 IDE 设备、保护逻辑、录播、数值召回等；本服务**不修改** ct-screen 表结构与数据（生产环境账号应仅 SELECT）。实体映射见项目根 `schema.sql`。

## 环境配置

```
application.yml         # 基础项：端口、应用名、激活 profile
application-dev.yml     # 开发环境 zeta.* 行为配置
application-prod.yml    # 生产环境（单机，读 /opt/zeta/config/）
dev/mysql.yml           # 开发双数据源（复制 mysql.yml.example）
deploy/config/*.example # 生产配置模板
deploy/systemd/         # systemd unit
```

### 配置分层

| 文件 | 内容 |
|------|------|
| `application.yml` | `server.*`、`spring.application.name`、`spring.profiles.active` |
| `application-dev.yml` | CORS、数据源策略、种子数据、上传目录 |
| `application-prod.yml` | 本机 127.0.0.1、绝对路径 uploads、ddl-auto none、关闭种子 |
| `dev/mysql.yml` / `/opt/zeta/config/mysql.yml` | 双数据源连接 |

### mysql.yml 示例（仅连接信息）

```yaml
zeta:
  datasource:
    business:
      url: jdbc:mysql://.../ct-screen-monitor?...
      username: ...
      password: ...
    screen:
      url: jdbc:mysql://.../ct-screen?...
      username: ...
      password: ...
```

方言使用 **MariaDB103Dialect**，兼容 MySQL 8 开发与 MariaDB 10.5.8 生产。

## 启动

```bash
# 开发
mvn spring-boot:run

# 生产（见 deploy/README.md）
java -jar zeta-server.jar --spring.profiles.active=prod
```

## 测试账号

| 用户名 | 密码 | 角色 |
|--------|------|------|
| student | 123456 | STUDENT |
| teacher | 123456 | TEACHER |
| admin | 123456 | ADMIN |

业务库首次启动自动初始化用户；展示条目在 ct-screen 存在对应屏柜/设备时写入业务库。

## Redis 队列（骨架）

| 方向 | 默认 Key |
|------|----------|
| 屏柜 → 业务 | `ct:screen:business:inbound` |
| 业务 → 屏柜 | `ct:business:screen:outbound` |

消息体见 `ScreenQueueMessage`（`type` + `payload`）。具体 type 约定待屏柜侧协议确定后补充。

## API 概览

### 业务（ct-screen-monitor）

- `/api/auth/*` — 鉴权
- `/api/users/*` — 用户管理（ADMIN）
- `/api/cabinets/{id}/display-items` — 屏柜学习条目 CRUD（图片 + 文字描述，ADMIN）
- `POST /api/admin/cabinet-display-images` — 上传认知图片（ADMIN）
- `POST /api/admin/cognition-videos` — 上传认知 MP4 视频（ADMIN，最大 50MB）
- `GET /api/videos/{type}/{id}` — 按认知条目读取视频（支持 Range 请求）
- `/api/devices/{id}/display-items` — 设备展示条目 CRUD（ADMIN，id 为屏柜库 device 主键）
- `/api/knowledge/devices/{id}/display-items` — 学员可读展示条目

### 屏柜只读（ct-screen）

- `/api/knowledge/*` — 知识结构树
- `/api/protection-logics/*` — 保护逻辑详情与断面

## 跨库引用

`device_display_items.screen_device_id` 引用 ct-screen 设备主键，无外键。创建展示条目前会只读校验屏柜库中设备是否存在。

## 本地开发（无屏柜系统）

1. 创建库 `ct-screen-monitor`、`ct-screen`
2. 配置 `dev/mysql.yml`（双库连接）
3. 在 ct-screen 中准备屏柜/设备/逻辑数据（由屏柜系统维护）
4. 启动服务

收到屏柜侧 SQL 后，将 ct-screen 实体与 Repository 对齐即可，业务侧只读层无需改库。
