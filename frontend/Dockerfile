# ---- 构建阶段 ----
FROM node:24-alpine AS build
WORKDIR /app
# 国内 npm 镜像（npmmirror），加速依赖下载
RUN npm config set registry https://registry.npmmirror.com
COPY package*.json ./
RUN npm ci
COPY . .
RUN npm run build

# ---- 运行阶段（Nginx 托管静态资源 + 反代 /api） ----
FROM nginx:alpine
COPY nginx.conf /etc/nginx/conf.d/default.conf
COPY --from=build /app/dist /usr/share/nginx/html
EXPOSE 80
