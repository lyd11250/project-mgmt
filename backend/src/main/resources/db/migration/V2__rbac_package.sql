-- =====================================================================
-- V2: RBAC 套餐化重构（第 1 期-A2）
-- 模型：① 全局菜单目录 sys_menu ② 套餐 sys_package(+sys_package_menu)
--       ③ 租户订阅 tenant.package_id ④ 租户内 RBAC：sys_role_menu
-- 全局表（sys_menu / sys_package / sys_package_menu）不带 tenant_id，
-- 已在多租户插件 IGNORE_TABLES 中；种子数据用固定小 BIGINT id（不与雪花大 id 冲突）。
-- 废弃 V1 的 sys_permission / sys_role_permission（权限不再每租户复制）。
-- =====================================================================

-- ---------------------------------------------------------------------
-- 全局菜单目录（菜单与按钮权限合并为一棵树）
-- ---------------------------------------------------------------------
CREATE TABLE sys_menu (
    id         BIGINT       PRIMARY KEY,
    parent_id  BIGINT       NOT NULL DEFAULT 0,
    type       VARCHAR(1)   NOT NULL,            -- M 目录 / C 菜单(页面) / F 按钮(权限)
    name       VARCHAR(64)  NOT NULL,
    path       VARCHAR(128),                     -- 路由路径（C 型）
    component  VARCHAR(128),                     -- 前端组件标识（C 型，如 system/UserList）
    icon       VARCHAR(64),
    perm       VARCHAR(128),                     -- 权限码 模块:资源:动作（C/F 型）
    sort       INT          NOT NULL DEFAULT 0,
    visible    SMALLINT     NOT NULL DEFAULT 1,  -- 是否在导航显示
    status     SMALLINT     NOT NULL DEFAULT 1,  -- 1 启用 0 停用
    created_by BIGINT,
    created_at TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_by BIGINT,
    updated_at TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted    SMALLINT     NOT NULL DEFAULT 0
);
CREATE INDEX idx_sys_menu_parent ON sys_menu (parent_id);
COMMENT ON TABLE sys_menu IS '全局菜单目录（平台维护，全局唯一，菜单+按钮权限一棵树）';

-- ---------------------------------------------------------------------
-- 套餐
-- ---------------------------------------------------------------------
CREATE TABLE sys_package (
    id         BIGINT       PRIMARY KEY,
    name       VARCHAR(64)  NOT NULL,
    code       VARCHAR(64)  NOT NULL,
    status     SMALLINT     NOT NULL DEFAULT 1,
    remark     VARCHAR(255),
    created_by BIGINT,
    created_at TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_by BIGINT,
    updated_at TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted    SMALLINT     NOT NULL DEFAULT 0
);
CREATE UNIQUE INDEX uk_sys_package_code ON sys_package (code) WHERE deleted = 0;
COMMENT ON TABLE sys_package IS '套餐（平台定义，= 菜单子集）';

-- 套餐 ↔ 菜单（多对多）
CREATE TABLE sys_package_menu (
    id         BIGINT    PRIMARY KEY,
    package_id BIGINT    NOT NULL,
    menu_id    BIGINT    NOT NULL,
    created_by BIGINT,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted    SMALLINT  NOT NULL DEFAULT 0
);
CREATE UNIQUE INDEX uk_sys_package_menu ON sys_package_menu (package_id, menu_id) WHERE deleted = 0;
COMMENT ON TABLE sys_package_menu IS '套餐-菜单关联（全局）';

-- ---------------------------------------------------------------------
-- 租户内 RBAC：角色 ↔ 菜单（取代 V1 的 sys_role_permission）
-- ---------------------------------------------------------------------
CREATE TABLE sys_role_menu (
    id         BIGINT    PRIMARY KEY,
    tenant_id  BIGINT    NOT NULL,
    role_id    BIGINT    NOT NULL,
    menu_id    BIGINT    NOT NULL,
    created_by BIGINT,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted    SMALLINT  NOT NULL DEFAULT 0
);
CREATE UNIQUE INDEX uk_sys_role_menu ON sys_role_menu (tenant_id, role_id, menu_id) WHERE deleted = 0;
COMMENT ON TABLE sys_role_menu IS '角色-菜单分配（租户内 RBAC）';

-- ---------------------------------------------------------------------
-- 租户订阅套餐
-- ---------------------------------------------------------------------
ALTER TABLE tenant ADD COLUMN package_id BIGINT;
COMMENT ON COLUMN tenant.package_id IS '订阅的套餐 id（圈定可用功能边界；平台超管通配绕过）';

-- ---------------------------------------------------------------------
-- 废弃旧权限表
-- ---------------------------------------------------------------------
DROP TABLE sys_role_permission;
DROP TABLE sys_permission;

-- =====================================================================
-- 种子：系统菜单树（perm = 模块:资源:动作）
-- =====================================================================
-- 目录
INSERT INTO sys_menu (id, parent_id, type, name, path, component, icon, perm, sort, visible) VALUES
  (1,  0, 'M', '系统管理', '/system', NULL, 'Setting', NULL, 1, 1);

-- 用户管理（C + 按钮 F）
INSERT INTO sys_menu (id, parent_id, type, name, path, component, icon, perm, sort, visible) VALUES
  (11,  1, 'C', '用户管理', '/system/users', 'system/UserList', 'User', 'system:user:list', 1, 1),
  (111, 11, 'F', '新增用户',   NULL, NULL, NULL, 'system:user:create',     1, 1),
  (112, 11, 'F', '编辑用户',   NULL, NULL, NULL, 'system:user:update',     2, 1),
  (113, 11, 'F', '删除用户',   NULL, NULL, NULL, 'system:user:delete',     3, 1),
  (114, 11, 'F', '重置密码',   NULL, NULL, NULL, 'system:user:resetPwd',   4, 1),
  (115, 11, 'F', '分配角色',   NULL, NULL, NULL, 'system:user:assignRole', 5, 1);

-- 角色管理
INSERT INTO sys_menu (id, parent_id, type, name, path, component, icon, perm, sort, visible) VALUES
  (12,  1, 'C', '角色管理', '/system/roles', 'system/RoleList', 'UserFilled', 'system:role:list', 2, 1),
  (121, 12, 'F', '新增角色', NULL, NULL, NULL, 'system:role:create',     1, 1),
  (122, 12, 'F', '编辑角色', NULL, NULL, NULL, 'system:role:update',     2, 1),
  (123, 12, 'F', '删除角色', NULL, NULL, NULL, 'system:role:delete',     3, 1),
  (124, 12, 'F', '分配菜单', NULL, NULL, NULL, 'system:role:assignMenu', 4, 1);

-- 菜单管理（仅平台超管，通过通配可见，不入普通套餐）
INSERT INTO sys_menu (id, parent_id, type, name, path, component, icon, perm, sort, visible) VALUES
  (13,  1, 'C', '菜单管理', '/system/menus', 'system/MenuList', 'Menu', 'system:menu:list', 3, 1),
  (131, 13, 'F', '新增菜单', NULL, NULL, NULL, 'system:menu:create', 1, 1),
  (132, 13, 'F', '编辑菜单', NULL, NULL, NULL, 'system:menu:update', 2, 1),
  (133, 13, 'F', '删除菜单', NULL, NULL, NULL, 'system:menu:delete', 3, 1);

-- 套餐管理（仅平台超管）
INSERT INTO sys_menu (id, parent_id, type, name, path, component, icon, perm, sort, visible) VALUES
  (14,  1, 'C', '套餐管理', '/system/packages', 'system/PackageList', 'Box', 'system:package:list', 4, 1),
  (141, 14, 'F', '新增套餐', NULL, NULL, NULL, 'system:package:create',     1, 1),
  (142, 14, 'F', '编辑套餐', NULL, NULL, NULL, 'system:package:update',     2, 1),
  (143, 14, 'F', '删除套餐', NULL, NULL, NULL, 'system:package:delete',     3, 1),
  (144, 14, 'F', '分配菜单', NULL, NULL, NULL, 'system:package:assignMenu', 4, 1);

-- 租户管理（仅平台超管）
INSERT INTO sys_menu (id, parent_id, type, name, path, component, icon, perm, sort, visible) VALUES
  (15,  1, 'C', '租户管理', '/system/tenants', 'system/TenantList', 'OfficeBuilding', 'system:tenant:list', 5, 1),
  (151, 15, 'F', '新增租户', NULL, NULL, NULL, 'system:tenant:create', 1, 1);

-- =====================================================================
-- 种子：套餐
-- =====================================================================
INSERT INTO sys_package (id, name, code, status, remark) VALUES
  (1, '基础版', 'BASIC', 1, '用户管理 + 角色管理'),
  (2, '全功能', 'FULL',  1, '全部系统菜单（平台租户）');

-- 基础版：用户管理 + 角色管理 子树（含目录 1）
INSERT INTO sys_package_menu (id, package_id, menu_id)
SELECT m.id, 1, m.id FROM sys_menu m
WHERE m.id IN (1, 11,111,112,113,114,115, 12,121,122,123,124);

-- 全功能：全部菜单（用 menu.id 作 package_menu.id 偏移，避免与基础版冲突）
INSERT INTO sys_package_menu (id, package_id, menu_id)
SELECT 1000 + m.id, 2, m.id FROM sys_menu m;
