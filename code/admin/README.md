# Zeta Admin（业务系统前端）

## 管理端目录

```
src/pages/admin/
├── business/     业务模块：用户、屏柜展示维护…
└── screen/       屏柜数据（只读）：列举 ct-screen 屏柜/设备/逻辑
```

侧边栏分为 **业务** 与 **屏柜** 两个区块。

## 路由

| 路径 | 模块 |
|------|------|
| `/admin/users/*` | business |
| `/admin/display/*` | business（展示条目维护） |
| `/admin/screen/cabinets/*` | screen（只读 catalog） |

兼容重定向：`/admin/presentation/*` → `/admin/display`

## 启动

```bash
cd ../server && mvn spring-boot:run
npm install && npm run dev
```
