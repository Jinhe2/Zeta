# V2 · ReactFlow DAG 自动布局引擎

基于 **React + TypeScript + ReactFlow** 的只读 DAG 结构图渲染引擎，遵循 [v2.md](../../../v2.md) 规范。

## 技术栈

- `@xyflow/react` — 图渲染
- TypeScript Strict Mode
- 自研 Sugiyama 风格布局（不依赖 G6/X6/GoJS）

## 目录结构

```text
src/v2/
├─ graph/           # 布局算法
│  ├─ buildGraph.ts
│  ├─ topoSort.ts
│  ├─ calculateLayers.ts
│  ├─ crossingReduction.ts
│  ├─ layout.ts
│  ├─ edgeRouting.ts
│  └─ types.ts
├─ nodes/           # ReactFlow 节点
├─ components/      # GraphView + 正交边
└─ demo/            # 配置适配与 Demo 数据
```

## 布局原理

1. **Layer Assignment**：Input 固定 Layer 0；其余节点 `layer = max(parent.layer) + 1`；Output 自然落在最右层。
2. **Crossing Minimization**：Barycenter 算法，自上而下 + 自下而上交替，4 轮迭代。
3. **Coordinate Assignment**：`x = layer × (220 + 350)`，`y = rank × (80 + 120)`，保证节点不重叠。
4. **Fan-Out / Fan-In**：自动插入隐藏 `__split__` / `__merge__` 辅助节点，仅参与布局，不渲染。
5. **Orthogonal Routing**：H-V-H 正交折线；若穿节点则 V-H-V 绕行。

## 运行

```bash
cd demo/mvp
npm run dev
# 打开 http://localhost:5173/v2
```

## 数据格式

内部使用 `GraphData`（nodes + edges）。继电保护 JSON 通过 `adaptZetaConfig()` 自动转换。
