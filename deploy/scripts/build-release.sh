#!/usr/bin/env bash
# 在仓库根目录执行：./deploy/scripts/build-release.sh
# 产出：deploy/dist/zeta-<version>/ 及 deploy/dist/zeta-<version>.tar.gz
set -euo pipefail

ROOT="$(cd "$(dirname "$0")/../.." && pwd)"
CODE="$ROOT/code"
DEPLOY="$ROOT/deploy"
DIST="$DEPLOY/dist"

VERSION="$(sed -n '/<artifactId>zeta-server<\/artifactId>/{n;s/.*<version>\([^<]*\)<\/version>.*/\1/p;q;}' "$CODE/server/pom.xml")"
if [[ -z "$VERSION" ]]; then
  echo "无法从 pom.xml 读取版本号" >&2
  exit 1
fi
RELEASE_NAME="zeta-${VERSION}"
RELEASE_DIR="$DIST/$RELEASE_NAME"

echo "==> 版本: $VERSION"
echo "==> 构建后端 JAR"
mvn -f "$CODE/server/pom.xml" -DskipTests package

echo "==> 构建前端 admin"
(cd "$CODE/admin" && npm ci && npm run build)

echo "==> 组装交付包: $RELEASE_DIR"
rm -rf "$RELEASE_DIR"
mkdir -p "$RELEASE_DIR"/{bin,web/admin,config,systemd,nginx,scripts}

cp "$CODE/server/target/zeta-server-${VERSION}.jar" "$RELEASE_DIR/bin/zeta-server.jar"
rsync -a --delete "$CODE/admin/dist/" "$RELEASE_DIR/web/admin/"

cp "$DEPLOY/config/"*.example "$RELEASE_DIR/config/"
cp "$DEPLOY/systemd/zeta-server.service" "$RELEASE_DIR/systemd/"
cp "$DEPLOY/nginx/zeta.conf.example" "$RELEASE_DIR/nginx/"
cp "$DEPLOY/scripts/install.sh" "$RELEASE_DIR/scripts/"
cp "$DEPLOY/scripts/upgrade.sh" "$RELEASE_DIR/scripts/"
cp "$DEPLOY/README.md" "$RELEASE_DIR/README.md"
cp "$DEPLOY/ARCHITECTURE.md" "$RELEASE_DIR/ARCHITECTURE.md"
echo "$VERSION" > "$RELEASE_DIR/VERSION"

chmod +x "$RELEASE_DIR/scripts/"*.sh

echo "==> 打包 tar.gz"
mkdir -p "$DIST"
tar -czf "$DIST/${RELEASE_NAME}.tar.gz" -C "$DIST" "$RELEASE_NAME"

echo ""
echo "交付物已就绪："
echo "  目录: $RELEASE_DIR"
echo "  压缩包: $DIST/${RELEASE_NAME}.tar.gz"
