# 前端多阶段构建：在容器（Linux）内完成 npm ci + build，产物由 nginx 托管。
# 构建在测试机 Docker 内进行，平台一致，规避了 Windows↔Linux 的 optional 原生依赖 lock 漂移问题。
# 用法：随 docker-compose `up -d --build` 自动构建，无需在本机出 dist 再上传。

# ---- 构建阶段 ----
FROM node:20-alpine AS build
WORKDIR /app
# 国内 npm 镜像加速依赖下载
RUN npm config set registry https://registry.npmmirror.com
COPY package.json package-lock.json ./
RUN npm ci
COPY . .
RUN npm run build

# ---- 运行阶段 ----
FROM nginx:alpine
COPY nginx.conf /etc/nginx/conf.d/default.conf
COPY --from=build /app/dist /usr/share/nginx/html
EXPOSE 80
