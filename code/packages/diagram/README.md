# @zeta/diagram

保护逻辑框图共享渲染包（V4 引擎）。

## 流水线

```
Zeta JSON → adaptZetaConfig → ELK 布局 → React Flow 渲染
```

## 使用

```tsx
import { ZetaGraphView } from '@zeta/diagram'

// 学员端只读 + 断面状态
<ZetaGraphView config={config} showDevInfo={false} nodeStates={states} />

// 管理端编辑
<ZetaGraphView
  config={config}
  showDevInfo={false}
  editable
  selectedNodeId={selectedId}
  onNodeSelect={setSelectedId}
  onConnect={handleConnect}
  onEdgeClick={handleEdgeClick}
/>
```

管理端通过 Vite alias 引用：`@zeta/diagram` → `code/packages/diagram/src`。
