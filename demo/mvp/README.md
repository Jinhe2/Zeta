# 继电保护逻辑框图渲染引擎 MVP

基于 `mannual/renderer-engine.md` 规范实现的逻辑框图渲染引擎演示项目。

## 功能

- 加载 `public/samples/` 目录下的样本 JSON（下拉选择，源自 `samples/fixed/`）
- 支持界面上传 JSON 文件
- **Legacy 引擎**（`/`）：初版布局
- **V1 引擎**（`/v1`）：DAG 拓扑排序 + 增量传播分层布局
- **V2 引擎**（`/v2`）：dagre 分层布局 + 边折线路由

## 路由

| 路径 | 引擎 | 说明 |
|------|------|------|
| `/` | Legacy | 原始 MVP 渲染 |
| `/v1` | V1 | Sugiyama 分层布局 + 规范连线路由 |
| `/v2` | V2 | **dagre** 分层布局 + 边折线路由 |

## 启动

```bash
cd demo/mvp
npm install
npm run dev
```

浏览器打开终端提示的地址（默认 `http://localhost:5173`）。

## 数据格式

支持 `samples/fixed/` 中的精简格式（无 `connections` 字段），运行时自动推导连线。

也兼容含 `connections` 字段的原始格式。

```json
{
  "inputs": [...],
  "gates": [...],
  "timers": [...],
  "outputs": [...],
  "settings": [...],
  "displayState": { "IN_XXX": "1" }
}
```

## 目录结构

```
src/
  engines/
    registry.js     # 引擎注册与路由
    legacy/         # Legacy 渲染引擎（独立）
    v1/             # V1 渲染引擎（独立）
    v2/             # V2 渲染引擎（独立）
    v3/             # V3 渲染引擎（独立）
    graph.js        # DAG 拓扑排序、叶节点序
    layerLayout.js  # 分层布局 + 增量传播
    wireRouter.js   # 规范连线路由
    index.js
  pages/
    LegacyPage.jsx  # /
    V1Page.jsx      # /v1
  components/
  hooks/
```

## 更新样本数据

将 `samples/fixed/` 中的文件同步到 `public/samples/`：

```bash
cp ../../samples/fixed/*.json public/samples/
```

## 构建

```bash
npm run build
npm run preview
```
