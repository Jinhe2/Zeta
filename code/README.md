# Zeta 正式工程

继电保护逻辑教学系统 — 管理后台、前台客户端、后端服务。

## 目录

| 目录 | 说明 | 技术栈 |
|------|------|--------|
| `admin/` | 管理后台（含学员/教师/管理员全部界面，按角色区分） | Vite + React + JavaScript |
| `front/` | 前台客户端（稍后开发） | Electron + Vite + React + JavaScript |
| `server/` | 后端 API 服务 | Java 8 + Spring Boot 2 + JPA + Redis |

## 知识结构

三层层级：**屏柜 → 设备 → 保护逻辑（装置）**；设备下另有 **设备认知条目（DeviceCognitionItem）**，用于多角度分条介绍设备。

```
220kV 线路保护屏（屏柜）
├── 线路保护装置 A（设备）
│   ├── 过流 I 段保护逻辑
│   └── 重合闸保护逻辑
└── 线路保护装置 B（设备）
    └── 差动保护逻辑
```

## 角色与界面

- **学员**：保护逻辑列表、逻辑框图、断面查看
- **教师**：教学相关功能（待扩展）
- **管理员**：系统管理功能（待扩展）

当前阶段由 `admin` 统一承载上述界面，通过登录角色进入不同路由；`front` 预留为后续独立客户端。

## 快速启动

### 后端

```bash
cd server
mvn spring-boot:run
```

默认端口 `8080`。开发环境使用 MySQL + Redis，配置见 `server/src/main/resources/dev/`（参考 `*.example` 文件）。

### 管理后台

```bash
cd admin
npm install
npm run dev
```

默认端口 `5173`，API 代理至 `http://localhost:8080`。

### 前台（暂未开发）

见 `front/README.md`。
