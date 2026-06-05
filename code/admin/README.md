# Zeta Admin

管理后台 Web 应用，统一承载学员、教师、管理员界面，按登录角色进入不同功能。

## 技术栈

- Vite
- React（JavaScript）
- React Router

## 启动

先启动后端，再启动本前端：

```bash
# 终端 1
cd ../server && mvn spring-boot:run

# 终端 2
npm install
npm run dev
```

访问 `http://localhost:5173/login`，使用用户名 + 密码登录。

## 角色路由

| 角色 | 路径 | 说明 |
|------|------|------|
| 学员 | `/student` | 保护逻辑列表、逻辑框图、断面 |
| 教师 | `/teacher` | 教学功能（待扩展） |
| 管理员 | `/admin` | 系统管理（待扩展） |

## 说明

- 逻辑框图渲染组件待从 `demo/mvp` 迁移或复用
- 当前已对接后端登录与保护逻辑 API
