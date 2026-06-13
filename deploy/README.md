# Zeta 业务系统 — 单机部署说明

> 架构总览见 [ARCHITECTURE.md](./ARCHITECTURE.md)（含部署拓扑、请求链路、模块与数据关系图）。

本文档面向**最终交付包**的使用方。交付物由构建脚本生成，包含后端 JAR、前端静态资源、配置模板与安装脚本。

---

## 1. 交付包内容

构建后在 `deploy/dist/zeta-<version>/`（及同名 `.tar.gz`）中包含：

```text
zeta-<version>/
├── VERSION                 # 版本号
├── README.md               # 部署说明
├── ARCHITECTURE.md         # 架构图（Mermaid）
├── bin/
│   └── zeta-server.jar     # Spring Boot 后端
├── web/
│   └── admin/              # 管理端/学员端 SPA（Vite build）
├── config/
│   ├── mysql.yml.example   # 双数据源（复制为 mysql.yml）
│   ├── redis.yml.example
│   └── jwt.yml.example
├── systemd/
│   └── zeta-server.service
├── nginx/
│   └── zeta.conf.example   # 反向代理示例
└── scripts/
    ├── install.sh          # 首次安装
    └── upgrade.sh          # 版本升级
```

运行时目录（安装后，默认 `/opt/zeta`）：

```text
/opt/zeta/
├── bin/zeta-server.jar
├── config/mysql.yml | redis.yml | jwt.yml   # 敏感配置，勿泄露
├── web/admin/
├── data/uploads/                            # 屏柜认知图片（需备份）
└── logs/zeta-server.log
```

---

## 2. 环境要求

| 组件 | 要求 |
|------|------|
| 操作系统 | Linux x86_64（推荐） |
| Java | **8+**（运行 JAR） |
| MariaDB | 10.5+ 或 MySQL 8.0+ |
| Redis | 6+（JWT 刷新令牌） |
| Nginx | 1.18+（对外 HTTP/HTTPS） |
| 构建机（仅打包时） | JDK 8、Maven 3.6+、Node.js 18+ |

数据库：

- `ct-screen-monitor` — 业务库（读写）
- `ct-screen` — 屏柜库（**只读**账号）

表结构见项目源码根目录 `schema-monitor.sql`、`schema.sql`（安装前在 DB 中执行）。

---

## 3. 构建交付包（开发/发布方）

在**仓库根目录**执行：

```bash
chmod +x deploy/scripts/build-release.sh
./deploy/scripts/build-release.sh
```

产出：

- `deploy/dist/zeta-<version>/` — 可整目录交付
- `deploy/dist/zeta-<version>.tar.gz` — 压缩包交付

---

## 4. 首次安装（现场运维）

### 4.1 解压

```bash
tar -xzf zeta-0.1.0.tar.gz
cd zeta-0.1.0
```

### 4.2 安装程序文件

```bash
chmod +x scripts/*.sh
sudo ./scripts/install.sh
```

可选环境变量：

| 变量 | 默认 | 说明 |
|------|------|------|
| `ZETA_INSTALL_ROOT` | `/opt/zeta` | 安装根目录 |
| `ZETA_RUN_USER` | `zeta` | 服务运行用户 |

### 4.3 配置

```bash
sudo vi /opt/zeta/config/mysql.yml   # 数据库连接
sudo vi /opt/zeta/config/redis.yml
sudo vi /opt/zeta/config/jwt.yml     # 务必更换 secret
sudo chmod 600 /opt/zeta/config/*.yml
sudo chown zeta:zeta /opt/zeta/config/*.yml
```

`application-prod.yml` 已打入 JAR，生产从 `/opt/zeta/config/` 读取上述文件。  
若使用域名访问，需在源码中构建前将 `zeta.cors.allowed-origins` 改为实际地址后重新打包，或后续版本支持外置覆盖。

### 4.4 初始化数据库

```bash
mysql -u root -p < schema-monitor.sql
mysql -u root -p < schema.sql
```

创建业务库读写用户、屏柜库只读用户，并与 `mysql.yml` 一致。

### 4.5 Nginx

```bash
sudo cp nginx/zeta.conf.example /etc/nginx/conf.d/zeta.conf
# 编辑 server_name、root、证书等
sudo nginx -t && sudo systemctl reload nginx
```

### 4.6 启动后端

```bash
sudo systemctl enable --now zeta-server
sudo systemctl status zeta-server
journalctl -u zeta-server -f
```

后端仅监听 **127.0.0.1:8080**，不应对公网直接暴露。

---

## 5. 版本升级

在新版本交付包目录下：

```bash
sudo ./scripts/upgrade.sh
```

会替换 `bin/zeta-server.jar` 与 `web/admin/`，**保留** `config/`、`data/uploads/`、`logs/`。升级前会自动备份至 `/opt/zeta/backup/<时间戳>/`。

---

## 6. 架构简述

```text
浏览器 → Nginx (:80/443)
           ├─ /           → /opt/zeta/web/admin  (静态 SPA)
           ├─ /api/       → 127.0.0.1:8080
           └─ /uploads/   → 127.0.0.1:8080  → /opt/zeta/data/uploads
```

---

## 7. 备份建议

| 内容 | 路径 |
|------|------|
| 业务数据 | MySQL `ct-screen-monitor` |
| 屏柜数据 | MySQL `ct-screen`（若本机维护） |
| 认知图片 | `/opt/zeta/data/uploads/` |
| 配置 | `/opt/zeta/config/` |

---

## 8. 常见问题

**Q: 上传图片失败？**  
检查 `/opt/zeta/data/uploads` 是否存在且 `zeta` 用户可写；Nginx `client_max_body_size` ≥ 10m。

**Q: 401 / 登录后立即失效？**  
检查 Redis 是否运行、`redis.yml` 是否正确。

**Q: 屏柜数据读不到？**  
检查 `ct-screen` 库是否有数据、screen 数据源账号是否有 SELECT 权限。

**Q: 生产会创建 student/admin 测试账号吗？**  
不会。`prod` profile 下 `zeta.seed.enabled=false`，需自行在库中创建用户或通过管理功能添加。

---

## 9. 联系与版本

交付包内 `VERSION` 文件记录当前版本号。构建时间与健康检查：

```bash
curl -s http://127.0.0.1:8080/api/auth/me   # 需带 Token；或查看 systemd 日志
```
