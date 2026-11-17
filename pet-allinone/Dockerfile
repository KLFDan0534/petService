# pet-service 一体化容器：nginx（前端静态 + 反向代理） + MinIO（对象存储）
# 单容器同时运行两个进程，对外暴露 80 / 9000 / 9001
#
# 设计说明：
#   - 运行时基座用官方 nginx:latest（本地已有，基于 Debian，自带 nginx）
#   - MinIO 二进制从 cgr.dev/chainguard/minio 镜像提取（上游 minio/minio 已归档下架）
#   - 不依赖 apt 安装 supervisor，改用 entrypoint.sh 统一管理两个进程

# ---------------------------------------------------------------------------
# 阶段 1：取出 minio 二进制
# ---------------------------------------------------------------------------
FROM cgr.dev/chainguard/minio:latest AS minio-bin

# ---------------------------------------------------------------------------
# 阶段 2：运行时镜像（nginx + minio + 自管理 entrypoint）
# ---------------------------------------------------------------------------
FROM nginx:latest

ENV TZ=Asia/Shanghai \
    MINIO_ROOT_USER=minioadmin \
    MINIO_ROOT_PASSWORD=minioadmin

# 拷贝 MinIO 二进制
COPY --from=minio-bin /usr/bin/minio /usr/local/bin/minio
RUN chmod +x /usr/local/bin/minio

# nginx 配置（覆盖主配置与站点配置）
COPY nginx/nginx.conf /etc/nginx/nginx.conf
COPY nginx/default.conf /etc/nginx/conf.d/default.conf

# 前端静态产物
COPY dist/ /usr/share/nginx/html/

# 启动脚本
COPY entrypoint.sh /usr/local/bin/entrypoint.sh
RUN chmod +x /usr/local/bin/entrypoint.sh

# MinIO 数据目录
VOLUME ["/data"]

EXPOSE 80 9000 9001

HEALTHCHECK --interval=30s --timeout=5s --start-period=20s --retries=3 \
    CMD curl -fsS http://127.0.0.1/ >/dev/null 2>&1 \
     && curl -fsS http://127.0.0.1:9000/minio/health/live >/dev/null 2>&1 \
     || exit 1

ENTRYPOINT ["/usr/local/bin/entrypoint.sh"]
