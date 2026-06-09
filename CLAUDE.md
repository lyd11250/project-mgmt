# CLAUDE.md

本文件指导任意 AI 编码助手（Claude 及其他模型）在本仓库中协作开发。**动手前请通读本文件**，它固化了本项目的设计决策、目录约定与编码规范。技术选型与架构见 [技术方案.md](技术方案.md)，业务域与功能设计见 [功能设计.md](功能设计.md)，开发计划与进度见 [开发进度.md](开发进度.md)，使用说明见 [README.md](README.md)。

---

## 1. 项目速览

**Bedrock — 多租户 SaaS 平台基座**：只做 SaaS 必备的系统能力（租户/套餐/RBAC/菜单/鉴权/多租户隔离），租户业务功能由上层业务模块（`biz/*`）扩展、**不属于基座**。模块化单体（当前不拆微服务）。

- **前端**：Vue3 + Vite + TypeScript + Pinia + Vue Router + Element Plus + Axios
- **后端**：Spring Boot **4.0.6**（Spring Framework 7 / JDK 17）+ MyBatis-Plus + Sa-token + Flyway
- **存储**：PostgreSQL 16+、Redis 7+（Sa-token 会话）
- **部署**：Docker Compose，区分 dev（测试机）/ prod（云服务器）
- **GitHub**：`lyd11250/project-mgmt`，主分支 `main`

**基座能力域**：租户、套餐、RBAC（用户/角色/菜单）、鉴权、多租户隔离 —— 归 `system` 模块。业务功能不内置，接入方式见 [功能设计.md](功能设计.md) 第 3 节。
**路线图**：见 [开发进度.md](开发进度.md)。当前：第 1 期-A 认证基座 ✅，下一步 **第 1 期-A2 基座完善与重命名**。

---

## 2. 关键约束（务必遵守，违反会导致构建/运行失败）

1. **本机无 Docker（但可编译）**：开发机有 JDK17 / Maven / Node，**可跑 `mvn compile/package`、`npm install/build`** 做编译验证；只是**无 Docker**，镜像构建与容器运行**全部在测试机**进行，不在本机执行 `docker` 命令。
2. **前端不在容器内构建**：前端在**本机 `npm run build` 出 `frontend/dist/`**，上传后由 `nginx:alpine` 容器只读挂载 `dist` + `nginx.conf` 托管（见 `docker-compose.yml` 的 `frontend` 服务）。**不再用 `npm ci` 在容器内构建**——避免跨平台（Windows 开发机 ↔ Linux 容器）解析 optional 原生依赖导致的 `package-lock.json` 漂移、`npm ci` 校验失败。`frontend/Dockerfile` 降级为「需要自包含镜像」的可选路径，默认编排不引用它。
3. **测试机网络在国内、慢**：直连 Docker Hub / Maven Central / npmjs 会卡死。已配国内源——后端 Maven 走 `backend/settings.xml`（阿里云镜像，构建命令带 `-s settings.xml`），前端 npm 走 `registry.npmmirror.com`（本机 `~/.npmrc` 或 `npm config`），Docker 拉基础镜像走测试机 `registry-mirrors`。**新增依赖时不要破坏这些换源配置。**
4. **Spring Boot 4 自动配置已模块化**：第三方库单独引入**不再触发自动配置**。集成 Flyway/Liquibase 等必须引 `spring-boot-starter-xxx`（如 `spring-boot-starter-flyway`），只引 `flyway-core` 会**静默失效**（不报错、不生效）。`flyway-database-postgresql` 仍需单独保留。参见 Spring Boot 4.0 Migration Guide。
5. **基础包为 `com.github.lyd11250.bedrock`**，系统能力在 `…bedrock.system` 包（含 controller/service/mapper/entity/dto/vo）。构件名 `saas-bedrock`（jar 为 `saas-bedrock.jar`）。新建类置于 `system` 域的正确子包。
6. **默认超管**：租户码 `PLATFORM` / 用户名 `admin` / 密码 `admin123456`（`PlatformSeeder` 幂等初始化，可用环境变量覆盖）。

---

## 3. 目录结构

```
backend/  Spring Boot（基础包 com.github.lyd11250.bedrock）
  common/   通用：Result / ResultCode / BusinessException / GlobalExceptionHandler / BaseEntity / GlobalBaseEntity
  config/   配置：SaToken / Cors / MybatisPlus / 多租户(TenantLineHandlerImpl + TenantContext) / 审计填充
  system/   系统能力域：租户/套餐/菜单/用户/角色/鉴权（含 PlatformSeeder / RbacConstants / StpInterfaceImpl + PingController）
  每个域内分层：controller / service / mapper / entity / dto / vo
  resources/ application.yml(+dev/prod) | db/migration/ (Flyway SQL)
frontend/ Vue3（src/ 下 api / stores / router / layouts / views / components / directives / utils）
docker-compose.yml(.dev/.prod)  容器编排
技术方案.md / 功能设计.md / 开发进度.md / README.md / CLAUDE.md  文档
```

---

## 4. 后端编码规范

### 4.1 分层与对象
- **分层**：`Controller → Service → Mapper(MyBatis-Plus)`，贫血模型。不引入 BO。
- **DTO**：接口入参（`xxxDTO`，放 `dto/`）；**VO**：接口出参（`xxxVO`，放 `vo/`）；**Entity**：持久层对象（放 `entity/`，`@TableName` 指定表名）。三者**手动互转**（当前在 Service 内写 `toVO` 私有方法，见 `UserService`），不要在 Entity 上直接出入参。
- **实体**继承 `common.BaseEntity`（含 `id / tenantId / 审计 / deleted`），子类加 `@EqualsAndHashCode(callSuper = true)`。**不要在实体里重复声明这些公共字段**。**全局表**（不带 `tenant_id`，如 `sys_menu` / `sys_package`）继承 `common.GlobalBaseEntity`（无 `tenantId`）；关联表若无 `updated_*` 列则显式声明字段（参照 `SysUserRole` / `SysRoleMenu`）。

### 4.2 统一响应
- Controller 一律返回 `Result<T>`：成功 `Result.ok(data)` / `Result.ok()`，失败抛异常（见下）。**不要手工 new 错误 Result**。
- 业务错误**抛 `BusinessException`**（`throw new BusinessException("用户名已存在")`），由 `GlobalExceptionHandler` 统一转 `{code, message, data}`。校验、未登录、无权限异常也已在全局处理器覆盖，无需在 Controller 内捕获。
- `ResultCode`：`0 成功 / 400 参数 / 401 未登录 / 403 无权限 / 404 不存在 / 500 内部错误`。

### 4.3 多租户（核心，易错）
- **业务代码无需手动过滤 `tenant_id`**：`TenantLineHandlerImpl` 自动给带 `tenant_id` 的表追加 `WHERE tenant_id = ?`，租户 ID 取自 Sa-token 当前会话。
- **新建业务表必须带 `tenant_id` 列**（`tenant` 表本身和 `flyway_schema_history` 例外，已在 `IGNORE_TABLES`）。新增不参与隔离的表时，记得加入 `IGNORE_TABLES`。
- 登录前定位用户、超管跨租户、建租户播种等场景，用 `TenantContext.runAs(tenantId, () -> {...})` 临时覆盖租户。取值优先级：`TenantContext 覆盖值 > Sa-token 会话 > 0`。

### 4.4 鉴权（Sa-token，套餐化 RBAC）
- 方法级权限：`@SaCheckPermission("system:user:create")`；角色级：`@SaCheckRole("SUPER_ADMIN")`。
- **权限码 DB 驱动，不再硬编码**：权限码即全局菜单 `sys_menu.perm`，格式 `模块:资源:动作`（如 `system:user:create`）。新增功能 → 在 Flyway 种子（或菜单管理页）注册菜单/按钮 perm → 纳入相应套餐 `sys_package_menu`。`RbacConstants` 仅保留角色码、套餐固定 id、通配 `*`。
- **鉴权判定**（`StpInterfaceImpl`）：用户权限 = 角色分配的菜单 perm（`sys_role_menu`）∩ 租户套餐边界（`tenant.package_id → sys_package_menu`）；含 `SUPER_ADMIN` 角色者通配 `*`，绕过边界。
- 角色码：`SUPER_ADMIN`（跨租户）/ `TENANT_ADMIN`（本租户套餐内全部）/ `USER`（只读页面）。
- 三层模型与菜单树详见 [功能设计.md](功能设计.md) 第 2 节。

### 4.5 持久层 & 查询
- 用 MyBatis-Plus `Wrappers.lambdaQuery()` 构建条件，分页用 `Page.of(current, size)`，返回 `IPage<VO>`（用 `page.convert(this::toVO)`）。
- 主键雪花算法 `IdType.ASSIGN_ID`（`application.yml` 全局已配）。软删除字段 `deleted`（`@TableLogic`，插件自动加 `deleted=0`）。
- 审计字段 `createdBy/At、updatedBy/At` 由 `AuditMetaObjectHandler` 自动填充，**勿手动赋值**。

### 4.6 数据库迁移（Flyway）
- 所有建表/变更写在 `backend/src/main/resources/db/migration/`，命名 `V{n}__{描述}.sql`（如 `V2__org_tables.sql`），版本号递增、**不可修改已合并的历史脚本**（Flyway 校验 checksum）。
- 建表约定（见 `V1` 头部注释）：主键 `BIGINT`（应用层生成）；除 `tenant` 外都带 `tenant_id`；统一审计列 + 软删除列 `deleted SMALLINT 0/1`；唯一索引带 `WHERE deleted = 0` 偏过滤；表/列加 `COMMENT`。

### 4.7 校验与命名
- 入参校验用 Jakarta Validation（`@NotBlank` 等）+ Controller 上 `@Valid`，错误消息写中文。
- Lombok：`@Data` / `@RequiredArgsConstructor`（构造器注入，**不要字段 `@Autowired`**）。
- 密码用注入的 `BCryptPasswordEncoder` 加密存 `passwordHash`，**绝不明文落库或返回**。

---

## 5. 前端编码规范

- **API 层**（`src/api/*.ts`）：每个域一个文件，函数式封装 `request.get/post/...`。后端响应已被拦截器解包，**调用方直接拿到 `data`**（`request` 响应拦截器对 `code===0` 返回 `body.data`，否则 `ElMessage` 报错并 reject）。分页返回 `PageResult<T>`（`records/total/current/size`）。
- **令牌**：键名 `satoken`（`TOKEN_KEY`，与后端 `sa-token.token-name` 一致），存 `localStorage`，请求拦截器自动附加；401 自动清 token 跳登录。
- **状态**：Pinia setup 风格（见 `stores/auth.ts`）。鉴权用 `useAuthStore`，`hasRole/hasPermission`（`*` 通配）判断。
- **路由**：`meta.public` 开放页；`meta.role` / `meta.permission` 控制访问，全局守卫已统一处理登录态与刷新后 `fetchMe`。新增页面在 `router/index.ts` 挂到 `DefaultLayout` 子路由并配 `meta`。
- **按钮级权限**：`v-permission="'user:create'"`（无权限移除元素）。
- **视图**：按域分目录（如 `views/system/`），用 Element Plus 组件。
- TS 严格类型，接口定义随 api 文件就近声明。
- **时间格式与国际化**：
  - 所有日期时间显示统一格式 `YYYY-MM-DD HH:mm:ss`，使用 `src/utils/time.ts` 中的 `formatDateTime()` 函数（基于 dayjs）。表格列显示时间时，用 `<template #default>{{ formatDateTime(row.createdAt) }}</template>` 的自定义模板方式。
  - Element Plus 已在 `main.ts` 配置中文 locale（`zh-cn`），所有日期选择器（`el-date-picker`）、时间选择器（`el-time-picker`）自动显示中文。新增日期选择器务必配置 `format="YYYY-MM-DD HH:mm:ss"` 与 `value-format="YYYY-MM-DDTHH:mm:ss"`（前者控制显示格式，后者控制存储格式）。

---

## 6. 构建与验证

### 本机（无 Docker，仅验证编译）
```bash
# 后端编译/打包（必须带 -s settings.xml 走阿里云源）
cd backend && ./mvnw -s settings.xml -DskipTests package   # 产物 target/saas-bedrock.jar
# 后端本地运行（需可达的 PG/Redis，默认 localhost，DB 名/用户/密码默认 project）
SPRING_PROFILES_ACTIVE=dev ./mvnw -s settings.xml spring-boot:run
# 健康检查：GET http://localhost:8080/api/v1/ping → {"code":0,"message":"成功","data":"pong"}

# 前端
cd frontend && npm install && npm run dev   # http://localhost:5173 （/api 代理到 8080）
```

### 测试机（Docker，dev）
```bash
# 前端改动：先在本机出产物再上传（容器只挂载、不构建）
cd frontend && npm run build            # 生成 frontend/dist/，连同仓库上传到测试机

# 测试机上执行（backend 镜像构建，frontend 挂载 dist）
docker compose -f docker-compose.yml -f docker-compose.dev.yml up -d --build
# 前端 :8088，后端 :8080（根路径返回 401 属正常，开放接口是 /api/v1/ping）
# 仅前端改动时无需 --build，重新上传 dist 后 `up -d` 重启 frontend 即可
```

**完成任意后端改动后，至少在本机跑通 `mvn -s settings.xml compile` 再提交；前端改动跑通 `npm run build` 再提交。**

---

## 7. Git 提交规范

- 提交信息用中文，遵循 Conventional Commits 前缀：`feat: / fix: / refactor: / chore: / docs:`，标题概括「做了什么」。
- 多文件大改动（如重命名、迁移）合并为单个语义化 commit，包迁移用 `git mv` 保留历史。
- **注意行尾**：仓库内文件多为 LF，批量 `sed/perl` 替换勿引入 CRLF 噪音（提交前用 `git diff -w --stat` 核对真实变动行数）。
- `push` 前先 `git diff --cached --stat` 自查范围；敏感配置（`.env`、密钥）不入库（已在 `.gitignore`）。
- 提交/推送等影响远程的操作，**先获用户确认**再执行。

---

## 8. 当前进度与开发计划

开发计划与实时进度统一维护在 **[开发进度.md](开发进度.md)**（单一事实来源）。开始任何开发前先读该文件确认所处阶段与任务拆解；完成一项后更新它，**无需改动本 CLAUDE.md**。

---

## 9. 给新接手模型的上手指引

若用户切换了模型，按以下顺序快速进入状态：
1. **先读五份文档**：本 `CLAUDE.md`（规范与约束）→ `技术方案.md`（技术选型与架构）→ `功能设计.md`（业务域与功能设计，含 RBAC 套餐模型）→ `开发进度.md`（计划与当前进度）→ `README.md`（启动方式与账号）。
2. **读一个完整样板域**：把系统能力域（`system` 包）从 `controller → service → entity → dto → vo → mapper` 通读一遍（推荐 `UserController` + `UserService`），它是所有模块的分层范式。再看 `common/` 与 `config/` 理解响应体、异常、多租户、审计、鉴权的统一机制。
3. **对照第 2 节关键约束**，尤其牢记：本机无 Docker、国内源、Boot4 starter 模块化、基础包路径。
4. **看 `git log --oneline`** 了解演进脉络。
5. 动手前，复杂任务先用计划/待办拆解，新业务域严格仿照 `system` 的分层与命名落地。
