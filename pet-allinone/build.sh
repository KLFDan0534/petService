#!/usr/bin/env bash
# pet-service 一体化镜像构建脚本（Git Bash / Linux / macOS）
set -e
cd "$(dirname "$0")"

echo "[1/3] 构建前端 (frontend -> dist) ..."
(cd ../frontend && npm run build)

echo "[2/3] 拷贝 dist 到构建上下文 ..."
rm -rf dist && cp -r ../frontend/dist dist

echo "[3/3] 构建镜像 pet-service:latest ..."
docker build -t pet-service:latest .

echo
echo "构建完成：pet-service:latest"
echo "启动：docker compose -f docker-compose.allinone.yml up -d"
