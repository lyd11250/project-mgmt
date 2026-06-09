# Bedrock — SaaS 平台基座

多租户 **SaaS 平台基座**：只提供 SaaS 必备的系统能力（**租户 / 套餐 / RBAC / 菜单 / 鉴权 / 多租户隔离**）；具体租户业务功能由上层业务模块（`biz/*`）扩展，**不属于基座**。

技术栈：Vue3 前端 + Spring Boot 4 后端 + PostgreSQL（MyBatis-Plus 多租户）+ Sa-token 鉴权 + Redis。

> 设计文档：技术选型与架构见 [技术方案.md](技术方案.md)，系统功能设计见 [功能设计.md](功能设计.md)，开发计划与进度见 [开发进度.md](开发进度.md)，协作规范见 [CLAUDE.md](CLAUDE.md)。
> 当前进度：第 1 期-A2 基座完善与重命名 ✅（已更名为 Bedrock，基础包 `…bedrock`、系统能力在 `system` 模块，RBAC 升级为「全局菜单 + 套餐订阅 + 租户内 RBAC」三层模型）。

## 技术栈

- **前端**：Vue3 + Vite + TypeScript + Pinia + Vue Router + Element Plus + Axios
- **后端**：Spring Boot 4.0（JDK 17）+ MyBatis-Plus + Sa-token + Flyway
- **存储**：PostgreSQL 16+、Redis 7+
- **部署**：Docker Compose（区分 dev / prod）

## 基座能力

- **租户管理**：租户（系统订阅方）、套餐订阅
- **RBAC**：用户、角色、菜单/权限、角色↔菜单分配
- **鉴权**：Sa-token 登录态 + 注解式角色/权限校验
- **多租户隔离**：MyBatis-Plus 插件按 `tenant_id` 自动隔离，业务代码无感

> 上层业务功能不内置，按 `biz/<module>/` 扩展，接入方式见 [功能设计.md](功能设计.md) 第 3 节。

## 初始账号与登录

- 首次启动时自动初始化「平台租户」与超级管理员（幂等，见 `PlatformSeeder`）。
- 默认超管：**租户编码 `PLATFORM` / 用户名 `admin` / 密码 `admin123456`**，可用环境变量 `APP_SUPERADMIN_USERNAME`、`APP_SUPERADMIN_PASSWORD` 覆盖。**生产首次登录后务必修改默认密码。**
- 登录采用 `租户编码 + 用户名 + 密码`（用户名在租户内唯一）。
- 角色：`SUPER_ADMIN`（跨租户，权限通配 `*`）/ `TENANT_ADMIN`（本租户全部）/ `USER`（只读）。

主要接口：`POST /api/v1/auth/login`（开放）、`POST /api/v1/auth/logout`、`GET /api/v1/auth/me`、`/api/v1/users`、`/api/v1/roles`、`/api/v1/tenants`（仅超管）。

## 目录结构

```
backend/    Spring Boot 后端（common / config / system〔当前 auth〕）
frontend/   Vue3 前端（api / stores / router / layouts / views）
docker-compose.yml(.dev/.prod)  容器编排
技术方案.md / 功能设计.md / 开发进度.md / CLAUDE.md  文档
```

## 环境

| 环境 | 用途 | 载体 | Profile / 模式 |
|------|------|------|----------------|
| dev | 开发 + 测试 | 测试机 | dev / development |
| prod | 生产 | 云服务器 | prod / production |

## 本地开发（不依赖 Docker）

前置：JDK 17、Node 20+、可访问的 PostgreSQL 与 Redis。

后端（需先建库并配置连接，默认连 localhost）：
```bash
cd backend
# 可用环境变量覆盖：DB_HOST/DB_PORT/DB_NAME/DB_USER/DB_PASSWORD/REDIS_HOST/REDIS_PORT
SPRING_PROFILES_ACTIVE=dev ./mvnw -s settings.xml spring-boot:run
# 验证：GET http://localhost:8080/api/v1/ping  →  {"code":0,"message":"成功","data":"pong"}
```

前端：
```bash
cd frontend
npm install
npm run dev      # http://localhost:5173 （/api 自动代理到 8080）
```

## 一键部署（Docker Compose）

> **前端部署模式**：前端**不在容器内构建**，而是在本机 `npm run build` 出 `frontend/dist/`，连同代码一起上传到测试机/云服务器，由 `nginx:alpine` 容器只读挂载 `dist` 与 `nginx.conf` 托管（反代 `/api` 到后端）。后端仍走容器内 Maven 构建。这样彻底规避跨平台 `npm ci` 的 lock 漂移问题。

```bash
# 1) 本机：构建前端产物（后端无需，本机/测试机均可编译；运行需容器）
cd frontend && npm install && npm run build   # 生成 frontend/dist/

# 2) 上传整个仓库（含 frontend/dist）到测试机/云服务器后，在其上执行：

# 测试机（开发/测试）：后端镜像构建，前端挂载 dist
docker compose -f docker-compose.yml -f docker-compose.dev.yml up -d --build

# 云服务器（生产）：先 cp .env.example .env 并修改密码
docker compose -f docker-compose.yml -f docker-compose.prod.yml up -d --build
```

- 前端代码改动后，需**重新 `npm run build` 并上传 `dist`**，容器无需 rebuild，`docker compose ... up -d` 重启 frontend 即可（或 nginx 直接读新文件）。
- dev：前端 `http://<测试机>:8088`，后端 `:8080`，PG `:5432`，Redis `:6379` 均暴露。
- prod：仅前端对外 `:80`（经 Nginx 反代 `/api` 到后端），DB/Redis 不暴露。
