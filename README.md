# 项目协作平台

多租户项目协作平台 — Vue3 前端 + Spring Boot 4 后端 + PostgreSQL（MyBatis-Plus 多租户）+ Sa-token 鉴权 + Redis。

> 详细设计见 [技术方案.md](技术方案.md)。当前进度：**第 1 期-A 认证权限基座**（登录 / RBAC / 用户与租户管理）。

## 技术栈

- **前端**：Vue3 + Vite + TypeScript + Pinia + Vue Router + Element Plus + Axios
- **后端**：Spring Boot 4.0（JDK 17）+ MyBatis-Plus + Sa-token + Flyway
- **存储**：PostgreSQL 16+、Redis 7+
- **部署**：Docker Compose（区分 dev / prod）

## 业务域（规划）

- 用户与权限（Sa-token 鉴权、RBAC、多租户隔离）
- 主数据管理（企业、人员、相关方）
- 项目进度管理（项目、里程碑、进度）

## 初始账号与登录

- 首次启动时自动初始化「平台租户」与超级管理员（幂等，见 `PlatformSeeder`）。
- 默认超管：**租户编码 `PLATFORM` / 用户名 `admin` / 密码 `admin123456`**，可用环境变量 `APP_SUPERADMIN_USERNAME`、`APP_SUPERADMIN_PASSWORD` 覆盖。**生产首次登录后务必修改默认密码。**
- 登录采用 `租户编码 + 用户名 + 密码`（用户名在租户内唯一）。
- 用户由管理员后台创建：超管在「租户管理」建租户并自动生成其租户管理员；租户管理员在「用户管理」建本租户普通用户并分配角色。
- 角色：`SUPER_ADMIN`（跨租户，权限通配 `*`）/ `TENANT_ADMIN`（本租户全部）/ `USER`（只读）。

主要接口：`POST /api/v1/auth/login`（开放）、`POST /api/v1/auth/logout`、`GET /api/v1/auth/me`、`/api/v1/users`、`/api/v1/roles`、`/api/v1/tenants`（仅超管）。

## 目录结构

```
backend/    Spring Boot 后端（common/config/auth/org/project/system）
frontend/   Vue3 前端（api/stores/router/layouts/views）
docker-compose.yml(.dev/.prod)  容器编排
技术方案.md  技术设计文档
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
SPRING_PROFILES_ACTIVE=dev ./mvnw spring-boot:run
# 验证：GET http://localhost:8080/api/v1/ping  →  {"code":0,"message":"成功","data":"pong"}
```

前端：
```bash
cd frontend
npm install
npm run dev      # http://localhost:5173 （/api 自动代理到 8080）
```

## 一键部署（Docker Compose）

```bash
# 测试机（开发/测试）
docker compose -f docker-compose.yml -f docker-compose.dev.yml up -d --build

# 云服务器（生产）：先 cp .env.example .env 并修改密码
docker compose -f docker-compose.yml -f docker-compose.prod.yml up -d --build
```

- dev：前端 `http://<测试机>:8088`，后端 `:8080`，PG `:5432`，Redis `:6379` 均暴露。
- prod：仅前端对外 `:80`（经 Nginx 反代 `/api` 到后端），DB/Redis 不暴露。
