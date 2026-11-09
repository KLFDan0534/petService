# pet-web：纯前端镜像（nginx 静态托管 + 反向代理）
#
# 与 pet-allinone 的区别：
#   - 不含 MinIO，只跑 nginx 一个进程
#   - 后端、MinIO 全部通过 host.docker.internal 访问宿主机
#     => 与基础设施容器完全解耦，可单独 up / down
#
# 构建前必须先有 dist/，由 build.sh / build.bat 生成（内含 npm run build）

FROM nginx:latest

ENV TZ=Asia/Shanghai

COPY nginx/nginx.conf /etc/nginx/nginx.conf
COPY nginx/default.conf /etc/nginx/conf.d/default.conf

# 前端静态产物（Vue3 + Vite 构建结果）
COPY dist/ /usr/share/nginx/html/

EXPOSE 80
