# 数据目录结构

## 目录说明

- **origin/** - 外部提供的原始 JSON 配置文件（包含冗余的 `connections` 字段）
- **fixed/** - 处理后的精简配置文件（移除 `connections`，由运行时推导）
- **scripts/** - 数据处理脚本

## 工作流程

### 1. 添加新配置

将外部提供的 JSON 文件放入 `origin/` 目录。

### 2. 转换配置

运行转换脚本，移除冗余的 `connections` 字段：

```bash
cd demo
node data/scripts/convert.js
```

脚本会：

- 读取 `origin/` 中的所有 JSON 文件
- 移除 `connections` 字段（因为可以从 `gates[].inputs`、`timers[].input`、`outputs[].input` 推导）
- 将处理后的文件保存到 `fixed/`

### 3. 运行时使用

**重要：** `data/` 目录是源数据仓库，不直接被项目引用。各项目根据需要或指令，将数据拷贝到自己的本地目录中使用。

#### 各项目本地数据目录

- **mvp** - `mvp/samples/*.json`
- **mvp2** - `mvp2/public/samples/*.json`
- **comparison** - `comparison/samples/*.json`

#### 拷贝数据到项目

转换完成后，根据需要将 `fixed/` 中的文件拷贝到目标项目：

```bash
# 拷贝到 mvp
cp data/fixed/*.json mvp/samples/

# 拷贝到 mvp2
cp data/fixed/*.json mvp2/public/samples/

# 拷贝到 comparison
cp data/fixed/*.json comparison/samples/
```

#### 运行时推导

项目运行时从本地 `samples/` 目录读取配置，并在需要时自动推导 `connections`（使用各项目的 `deriveConnections.js`）。

### 4. 推导规则

`connections` 字段是冗余的，可以从以下字段推导：

```javascript
// 从 gates[].inputs 推导
for (const gate of config.gates || []) {
  (gate.inputs || []).forEach((inp, idx) => {
    const fromId = typeof inp === 'object' ? inp.node : inp;
    connections.push({ from: fromId, to: gate.id, toInputIndex: idx });
  });
}

// 从 timers[].input 推导
for (const timer of config.timers || []) {
  if (timer.input) {
    connections.push({ from: timer.input, to: timer.id, toInputIndex: 0 });
  }
}

// 从 outputs[].input 推导
for (const output of config.outputs || []) {
  if (output.input) {
    connections.push({ from: output.input, to: output.id, toInputIndex: 0 });
  }
}
```

## 文件对比

### origin/example.json（原始格式）

```json
{
  "version": "1.0",
  "name": "过流I段保护逻辑",
  "inputs": [...],
  "gates": [...],
  "timers": [...],
  "outputs": [...],
  "connections": [...]  // 冗余字段
}
```

### fixed/example.json（精简格式）

```json
{
  "version": "1.0",
  "name": "过流I段保护逻辑",
  "inputs": [...],
  "gates": [...],
  "timers": [...],
  "outputs": [...]
  // connections 已移除
}
```

## 当前配置

- `example.json` - 过流I段保护（移除 12 条 connections）
- `reclose.json` - 重合闸保护（移除 33 条 connections）
- `1.json` - 保护逻辑 1（移除 7 条 connections）
