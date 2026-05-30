#!/usr/bin/env node

/**
 * 转换外部 JSON 配置为精简格式
 * - 移除冗余的 connections 字段
 * - 统一 gate inputs 为对象格式 {node, inverted}
 *
 * 用法: node data/scripts/convert.js
 */

const fs = require('fs');
const path = require('path');

const ORIGIN_DIR = path.join(__dirname, '..', 'origin');
const FIXED_DIR = path.join(__dirname, '..', 'fixed');

/**
 * 统一 gate inputs 为对象格式 {node, inverted}
 * - 字符串 "id" → {node: "id", inverted: false}
 * - 对象 {node} → {node, inverted: false}（缺失 inverted 时）
 * - 对象 {node, inverted} → 保持不变
 */
function normalizeGateInputs(gates) {
  return (gates || []).map(gate => ({
    ...gate,
    inputs: (gate.inputs || []).map(inp => {
      if (typeof inp === 'string') {
        return { node: inp, inverted: false };
      }
      return { node: inp.node, inverted: !!inp.inverted };
    }),
  }));
}

function convertConfig(config) {
  const { connections, gates, ...rest } = config;
  return {
    ...rest,
    gates: normalizeGateInputs(gates),
  };
}

function convertFile(filename) {
  const originPath = path.join(ORIGIN_DIR, filename);
  const raw = fs.readFileSync(originPath, 'utf-8');
  const config = JSON.parse(raw);
  const converted = convertConfig(config);

  const fixedPath = path.join(FIXED_DIR, filename);
  fs.writeFileSync(fixedPath, JSON.stringify(converted, null, 2), 'utf-8');

  const strCount = (config.gates || []).reduce(
    (n, g) => n + (g.inputs || []).filter(i => typeof i === 'string').length, 0
  );
  console.log(`✓ ${filename} - removed ${config.connections?.length || 0} connections, normalized ${strCount} string inputs`);
}

// Process all JSON files in origin/
const files = fs.readdirSync(ORIGIN_DIR).filter(f => f.endsWith('.json'));
files.forEach(convertFile);

console.log(`\nConverted ${files.length} files to data/fixed/`);
