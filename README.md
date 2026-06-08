# 项目协作平台

多租户项目协作平台 — Vue3 前端 + Spring Boot 后端 + PostgreSQL（MyBatis-Plus 多租户）+ Sa-token 鉴权 + Redis。

> 详细设计见 [技术方案.md](技术方案.md)。

## 技术栈

- **前端**：Vue3 + Vite + TypeScript + Pinia + Vue Router + Element Plus + Axios + ECharts
- **后端**：Spring Boot 3.x（JDK 17）+ MyBatis-Plus + Sa-token + Flyway
- **存储**：PostgreSQL 15+、Redis 7+
- **部署**：Docker Compose（区分 dev / prod）

## 业务域

- 用户与权限（Sa-token 鉴权、RBAC、多租户隔离）
- 主数据管理（企业、人员、相关方）
- 项目进度管理（项目、里程碑、进度）

## 环境与启动

| 环境 | 用途 | 载体 |
|------|------|------|
| dev | 开发 + 测试 | 测试机 |
| prod | 生产 | 云服务器 |

```bash
# 测试机（开发/测试）
docker compose -f docker-compose.yml -f docker-compose.dev.yml up -d

# 云服务器（生产）
docker compose -f docker-compose.yml -f docker-compose.prod.yml up -d
```

> 脚手架尚未生成，以上为目标形态。
