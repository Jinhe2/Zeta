# 快速部署指南

## 环境要求

- Java
- MySQL
- Redis
- Nginx（可选）

> 环境运行起来后，数据库部分需要把冷启动数据导入

## 后端运行

### 1. 准备配置文件

创建 `application-prod.yml`：

```yaml
zeta:
  datasource:
    business:
      url: jdbc:mysql://localhost:3306/ct-screen-monitor?useSSL=false&serverTimezone=GMT%2B8
      username: your_username
      password: your_password
      driver-class-name: com.mysql.cj.jdbc.Driver
      ddl-auto: update
    screen:
      url: jdbc:mysql://localhost:3306/ct-screen?useSSL=false&serverTimezone=GMT%2B8
      username: your_username
      password: your_password
      driver-class-name: com.mysql.cj.jdbc.Driver
      ddl-auto: none
      read-only: true

spring:
  redis:
    host: localhost
    port: 6379
    username: your_redis_username
    password: your_redis_password
    timeout: 3000ms

server:
  port: 8080
```

**数据库说明**：

- **ct-screen-monitor**（业务库）：存储用户、快照、认知条目等业务数据，可读写
- **ct-screen**（屏柜库）：存储屏柜、设备、保护逻辑等基础数据，只读

### 2. 启动后端

启动命令行，进入后端程序包目录，执行以下指令：

```bash
java -jar zeta-server-1.0.0.jar --spring.profiles.active=prod --spring.config.location=./application-prod.yml
```

启动成功标志：日志输出 `Started ZetaApplication in X.XXX seconds`

> 命令行程序不要关闭

## 前端部署

### 1. 解压前端文件

将 `dist.zip` 解压到 Web 服务器目录（如 Nginx 的 `/var/www/zeta/`）

### 2. 配置 Nginx（可选）

```nginx
server {
    listen 80;
    server_name _;

    location / {
        root /var/www/zeta;
        try_files $uri $uri/ /index.html;
    }

    location /api/ {
        proxy_pass http://localhost:8080;
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
    }
}
```

### 3. 直接打开（无 Nginx）

直接用浏览器打开 `dist/index.html` 文件（需配置后端地址）

## 客户端安装

1. 运行 `Zeta 教学系统 Setup 1.0.0.exe`
2. 按提示完成安装
3. 启动客户端，配置后端地址

## 配置后端地址

### Web 端（浏览器）

1. 打开登录页面
2. 点击右上角 **⚙** 按钮
3. 输入后端地址：`IP:端口`（如 `192.168.1.100:8080`）
4. 点击"测试连接"
5. 点击"保存"

### Electron 客户端

编辑安装目录下的 `settings.json`：

填入后端地址：`协议://IP:端口`（如 `http://192.168.1.100:8080`）

```json
{
  "apiBaseUrl": "http://192.168.1.100:8080"
}
```

重启客户端生效。

## 验证部署

- [ ] 后端日志显示 `Started ZetaApplication`
- [ ] 浏览器能访问前端页面
- [ ] 配置后端地址后，测试连接成功
- [ ] 使用 `admin/123456` 登录成功

## 默认账号

| 用户名  | 密码   | 角色   |
| ------- | ------ | ------ |
| student | 123456 | 学员   |
| teacher | 123456 | 教师   |
| admin   | 123456 | 管理员 |

---

**版本**：1.0.0
**更新日期**：2026-07-03
