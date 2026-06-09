# 前端部署：默认走 docker-compose 的 nginx 挂载模式（本机 npm run build 出 dist 后上传）。
# 本 Dockerfile 仅用于「需要把前端打成自包含镜像」的可选场景（如脱离 compose 单独分发）。
# 用法：先在本机 `npm run build` 生成 dist/，再 `docker build -t bedrock-frontend .`。
FROM nginx:alpine
COPY nginx.conf /etc/nginx/conf.d/default.conf
COPY dist /usr/share/nginx/html
EXPOSE 80
