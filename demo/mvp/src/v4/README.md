# V4 · ELK + ReactFlow 布局引擎

```
Graph JSON
    ↓
ELK Layout Engine（layered + orthogonal）
    ↓
nodes + edges positions
    ↓
ReactFlow 渲染
```

## 技术栈

- **elkjs** — Eclipse Layout Kernel（Layered 算法）
- **@xyflow/react** — 图渲染
- **TypeScript**

## Graph JSON 格式

```json
{
  "nodes": [
    { "id": "IN_A", "name": "输入 A", "type": "input" },
    { "id": "G1", "name": "AND", "type": "gate", "data": { "gateType": "AND" } },
    { "id": "OUT", "name": "动作", "type": "output" }
  ],
  "edges": [
    { "id": "e1", "source": "IN_A", "target": "G1" },
    { "id": "e2", "source": "G1", "target": "OUT" }
  ]
}
```

也支持继电保护 zeta 配置 JSON，会自动转换。

## ELK 布局参数

- `elk.algorithm`: layered
- `elk.direction`: RIGHT（Input 左 → Output 右）
- `elk.edgeRouting`: ORTHOGONAL
- Input / Output 层约束：FIRST / LAST

## 运行

```bash
cd demo/mvp && npm run dev
# http://localhost:5173/v4
```
