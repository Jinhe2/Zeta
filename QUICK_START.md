# 快速部署指南

## 环境要求

| 组件 | 版本要求 | 用途 |
|------|---------|------|
| Java | 8+ | 后端运行环境 |
| MySQL | 8.0+ | 业务数据库 |
| Redis | 6.0+ | 缓存和消息队列 |
| Node.js | 18+ | 前端构建（可选） |
| Nginx | 任意稳定版 | 前端静态文件托管（可选） |

## 后端部署

### 1. 构建

```bash
cd code/server
mvn clean package -DskipTests
```

构建产物：`target/zeta-server-1.0.0.jar`

### 2. 配置

编辑 `src/main/resources/application-prod.yml`：

```yaml
spring:
  datasource:
    url: jdbc:mysql://localhost:3306/zeta_business
    username: your_username
    password: your_password
  redis:
    host: localhost
    port: 6379
    password: your_redis_password
```

### 3. 运行

```bash
java -jar target/zeta-server-1.0.0.jar --spring.profiles.active=prod
```

后端默认端口：`8080`

## 前端部署

### 方式一：开发模式

```bash
cd code/admin
npm install
npm run dev
```

访问：`http://localhost:5173`

### 方式二：生产构建

```bash
cd code/admin
npm install
npm run build
```

构建产物：`dist/` 目录

使用 Nginx 或其他 Web 服务器托管 `dist/` 目录下的静态文件。

## 客户端部署（Electron）

### 1. 构建

```bash
cd code/front
npm install
npm run build
```

构建产物：`dist/Zeta 教学系统 Setup x.x.x.exe`

### 2. 安装

将安装包分发到客户端机器，运行安装程序即可。

## 配置后端地址

### 方式一：Web 端（浏览器访问）

1. 打开登录页面
2. 点击右上角 **⚙** 按钮
3. 输入后端地址（格式：`IP:端口`，如 `192.168.1.100:8080`）
4. 点击"测试连接"验证
5. 点击"保存"

配置保存在浏览器 localStorage 中，重启后保留。

### 方式二：Electron 客户端

编辑安装目录下的 `settings.json`：

```json
{
  "apiBaseUrl": "http://192.168.1.100:8080"
}
```

重启客户端生效。

## 验证部署

1. 后端启动成功：日志显示 `Started ZetaApplication`
2. 前端访问正常：浏览器打开页面无报错
3. 连接测试通过：在设置中测试连接显示"连接成功"
4. 登录测试：使用默认账号 `admin/123456` 登录成功

## 默认账号

| 用户名 | 密码 | 角色 |
|--------|------|------|
| student | 123456 | 学员 |
| teacher | 123456 | 教师 |
| admin | 123456 | 管理员 |

---

**版本**：1.0.0  
**更新日期**：2026-07-03
