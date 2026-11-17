#!/bin/bash
# pet-service 一体化容器启动脚本
# 在同一容器内同时运行 nginx 与 MinIO，并负责信号转发与退出清理。
set -e

log() { echo "[entrypoint] $*"; }

# ---------- 环境变量默认值 ----------
export MINIO_ROOT_USER="${MINIO_ROOT_USER:-minioadmin}"
export MINIO_ROOT_PASSWORD="${MINIO_ROOT_PASSWORD:-minioadmin}"
MINIO_DATA_DIR="${MINIO_DATA_DIR:-/data}"
MINIO_CONSOLE_ADDR="${MINIO_CONSOLE_ADDR:-:9001}"

mkdir -p "$MINIO_DATA_DIR" /var/log/nginx /run
log "MINIO_DATA_DIR=$MINIO_DATA_DIR"

PIDS=()

# ---------- 退出清理 ----------
shutdown() {
    log "收到退出信号，正在停止子进程 ..."
    for pid in "${PIDS[@]}"; do
        kill -TERM "$pid" 2>/dev/null || true
    done
    wait 2>/dev/null || true
    exit 0
}
trap shutdown TERM INT

# ---------- 启动 MinIO ----------
log "启动 MinIO (console ${MINIO_CONSOLE_ADDR}) ..."
/usr/local/bin/minio server "$MINIO_DATA_DIR" --console-address "$MINIO_CONSOLE_ADDR" &
PIDS+=($!)

# ---------- 启动 nginx ----------
log "启动 nginx ..."
nginx -g 'daemon off;' &
PIDS+=($!)

log "pet-service 已启动：nginx(:80) + MinIO(:9000/:9001)，PID=${PIDS[*]}"

# ---------- 等待任一进程退出，则整体退出（容器可自动重启） ----------
wait -n
EXIT_CODE=$?
log "有子进程退出 (code=$EXIT_CODE)，容器即将关闭。"
shutdown
