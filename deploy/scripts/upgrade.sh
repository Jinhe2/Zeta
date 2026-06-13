#!/usr/bin/env bash
# 升级已安装实例（替换 JAR 与前端，保留 config/ data/ logs/）
# 用法: sudo ./scripts/upgrade.sh
set -euo pipefail

if [[ "${EUID:-$(id -u)}" -ne 0 ]]; then
  echo "请使用 root 或 sudo 运行升级脚本" >&2
  exit 1
fi

RELEASE_ROOT="$(cd "$(dirname "$0")/.." && pwd)"
INSTALL_ROOT="${ZETA_INSTALL_ROOT:-/opt/zeta}"
RUN_USER="${ZETA_RUN_USER:-zeta}"
BACKUP_DIR="$INSTALL_ROOT/backup/$(date +%Y%m%d-%H%M%S)"

if [[ ! -f "$RELEASE_ROOT/bin/zeta-server.jar" ]]; then
  echo "未找到 bin/zeta-server.jar" >&2
  exit 1
fi

if [[ ! -f "$INSTALL_ROOT/bin/zeta-server.jar" ]]; then
  echo "未检测到已安装实例，请先运行 install.sh" >&2
  exit 1
fi

echo "==> 备份至 $BACKUP_DIR"
mkdir -p "$BACKUP_DIR"
cp -a "$INSTALL_ROOT/bin/zeta-server.jar" "$BACKUP_DIR/"
rsync -a "$INSTALL_ROOT/web/admin/" "$BACKUP_DIR/admin/"

echo "==> 停止服务"
systemctl stop zeta-server || true

echo "==> 更新程序文件"
install -m 644 "$RELEASE_ROOT/bin/zeta-server.jar" "$INSTALL_ROOT/bin/zeta-server.jar"
rsync -a --delete "$RELEASE_ROOT/web/admin/" "$INSTALL_ROOT/web/admin/"
chown -R "$RUN_USER:$RUN_GROUP" "$INSTALL_ROOT/bin" "$INSTALL_ROOT/web"

echo "==> 更新 systemd（若交付包有变更）"
if [[ -f "$RELEASE_ROOT/systemd/zeta-server.service" ]]; then
  install -m 644 "$RELEASE_ROOT/systemd/zeta-server.service" /etc/systemd/system/zeta-server.service
  systemctl daemon-reload
fi

echo "==> 启动服务"
systemctl start zeta-server
systemctl status zeta-server --no-pager || true

echo ""
echo "升级完成。配置与上传目录未改动: $INSTALL_ROOT/config $INSTALL_ROOT/data"
