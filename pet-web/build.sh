#!/usr/bin/env bash
# pet-web 前端镜像构建脚本（Git Bash / Linux / macOS）
# 只构建前端 + nginx，不涉及后端和 MinIO
set -e
cd "$(dirname "$0")"

echo "[1/3] 构建前端 (frontend -> dist) ..."
(cd ../frontend && npm run build)

echo "[2/3] 拷贝 dist 到构建上下文 ..."
rm -rf dist && cp -r ../frontend/dist dist

echo "[3/3] 构建镜像 pet-web:latest ..."
docker build -t pet-web:latest .

echo
echo "构建完成：pet-web:latest"
echo "启动：docker compose -f docker-compose.web.yml up -d"
echo "停止：docker compose -f docker-compose.web.yml down"
