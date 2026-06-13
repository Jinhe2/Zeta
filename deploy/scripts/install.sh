#!/usr/bin/env bash
# 首次安装（在交付包根目录执行，即含 bin/ web/ config/ 的目录）
# 用法: sudo ./scripts/install.sh
# 可选: sudo ZETA_INSTALL_ROOT=/opt/zeta ./scripts/install.sh
set -euo pipefail

if [[ "${EUID:-$(id -u)}" -ne 0 ]]; then
  echo "请使用 root 或 sudo 运行安装脚本" >&2
  exit 1
fi

RELEASE_ROOT="$(cd "$(dirname "$0")/.." && pwd)"
INSTALL_ROOT="${ZETA_INSTALL_ROOT:-/opt/zeta}"
RUN_USER="${ZETA_RUN_USER:-zeta}"
RUN_GROUP="${ZETA_RUN_GROUP:-zeta}"

echo "==> 安装目标: $INSTALL_ROOT"
echo "==> 运行用户: $RUN_USER"

if [[ ! -f "$RELEASE_ROOT/bin/zeta-server.jar" ]]; then
  echo "未找到 bin/zeta-server.jar，请在交付包根目录执行" >&2
  exit 1
fi

if ! id "$RUN_USER" &>/dev/null; then
  useradd -r -s /usr/sbin/nologin "$RUN_USER"
  echo "已创建系统用户: $RUN_USER"
fi

mkdir -p "$INSTALL_ROOT"/{bin,config,web/admin,data/uploads,logs}
chown -R "$RUN_USER:$RUN_GROUP" "$INSTALL_ROOT"

echo "==> 安装程序文件"
install -m 644 "$RELEASE_ROOT/bin/zeta-server.jar" "$INSTALL_ROOT/bin/zeta-server.jar"
rsync -a --delete "$RELEASE_ROOT/web/admin/" "$INSTALL_ROOT/web/admin/"

echo "==> 安装配置模板（不覆盖已有配置）"
for f in mysql.yml redis.yml jwt.yml; do
  src="$RELEASE_ROOT/config/${f}.example"
  dst="$INSTALL_ROOT/config/$f"
  if [[ -f "$src" ]]; then
    if [[ -f "$dst" ]]; then
      echo "  保留已有: $dst"
    else
      install -m 600 -o "$RUN_USER" -g "$RUN_GROUP" "$src" "$dst"
      echo "  已创建: $dst（请编辑后再启动服务）"
    fi
  fi
done

echo "==> 安装 systemd 单元"
install -m 644 "$RELEASE_ROOT/systemd/zeta-server.service" /etc/systemd/system/zeta-server.service
systemctl daemon-reload

if [[ -f "$RELEASE_ROOT/nginx/zeta.conf.example" ]]; then
  echo "==> Nginx 配置示例位于: $RELEASE_ROOT/nginx/zeta.conf.example"
  echo "    请复制到 /etc/nginx/conf.d/ 并按现场域名修改后 reload nginx"
fi

chown -R "$RUN_USER:$RUN_GROUP" "$INSTALL_ROOT"

echo ""
echo "安装完成。后续步骤："
echo "  1. 编辑 $INSTALL_ROOT/config/mysql.yml redis.yml jwt.yml"
echo "  2. 初始化数据库（schema-monitor.sql、schema.sql）"
echo "  3. 配置 Nginx（见交付包 nginx/zeta.conf.example）"
echo "  4. systemctl enable --now zeta-server"
echo "  5. systemctl status zeta-server"
