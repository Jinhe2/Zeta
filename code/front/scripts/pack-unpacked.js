#!/usr/bin/env node
/**
 * 将 electron-builder 输出的 unpacked 目录打包成单个压缩包
 *
 * 用法:
 *   npm run pack:unpacked                  # 默认打包 release/win-unpacked
 *   npm run pack:unpacked -- --format 7z   # 使用 7z 格式（需安装 7-Zip）
 *
 * 支持平台: Windows (PowerShell / 7-Zip), macOS/Linux (zip / 7-Zip)
 */

const { execSync } = require("child_process");
const path = require("path");
const fs = require("fs");

// ---------- 配置 ----------
const RELEASE_DIR = path.resolve(__dirname, "..", "release");
const UNPACKED_DIR = path.join(RELEASE_DIR, "win-unpacked");
const PKG = require("../package.json");
const OUTPUT_NAME = `${PKG.productName || PKG.name}-v${PKG.version}-portable`;

// ---------- 参数解析 ----------
const args = process.argv.slice(2);
const formatFlag = args.indexOf("--format");
const format = formatFlag !== -1 ? args[formatFlag + 1] : "zip";

// ---------- 前置检查 ----------
if (!fs.existsSync(UNPACKED_DIR)) {
  console.error(`❌ 未找到 unpacked 目录: ${UNPACKED_DIR}`);
  console.error("   请先运行: npm run build:dir");
  process.exit(1);
}

// ---------- 工具函数 ----------
function cmdExists(cmd) {
  try {
    execSync(
      process.platform === "win32"
        ? `where ${cmd} >nul 2>&1`
        : `which ${cmd} >/dev/null 2>&1`
    );
    return true;
  } catch {
    return false;
  }
}

function run(cmd, label) {
  console.log(`⏳ ${label} ...`);
  try {
    execSync(cmd, { stdio: "inherit" });
    console.log(`✅ 完成: ${outputPath}`);
  } catch {
    console.error(`❌ ${label} 失败`);
    process.exit(1);
  }
}

// ---------- 打包逻辑 ----------
let outputPath;

if (format === "7z") {
  if (!cmdExists("7z")) {
    console.error("❌ 未找到 7z，请安装 7-Zip: https://www.7-zip.org/");
    process.exit(1);
  }
  outputPath = path.join(RELEASE_DIR, `${OUTPUT_NAME}.7z`);
  if (fs.existsSync(outputPath)) fs.unlinkSync(outputPath);
  run(
    `7z a -t7z -mx=9 "${outputPath}" "${UNPACKED_DIR}${path.sep}*"` ,
    "正在用 7-Zip 压缩"
  );
} else {
  // 默认 zip
  if (process.platform === "win32") {
    outputPath = path.join(RELEASE_DIR, `${OUTPUT_NAME}.zip`);
    if (fs.existsSync(outputPath)) fs.unlinkSync(outputPath);
    run(
      `powershell -NoProfile -Command "Compress-Archive -Path '${UNPACKED_DIR}\\*' -DestinationPath '${outputPath}' -CompressionLevel Optimal"`,
      "正在用 PowerShell 压缩"
    );
  } else {
    outputPath = path.join(RELEASE_DIR, `${OUTPUT_NAME}.zip`);
    if (fs.existsSync(outputPath)) fs.unlinkSync(outputPath);
    run(
      `cd "${UNPACKED_DIR}" && zip -r -9 "${outputPath}" .`,
      "正在用 zip 压缩"
    );
  }
}

// ---------- 输出文件大小 ----------
const sizeMB = (fs.statSync(outputPath).size / 1024 / 1024).toFixed(2);
console.log(`📦 文件大小: ${sizeMB} MB`);
