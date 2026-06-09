-- =====================================================================
-- V5: 配额定义字典 + 套餐配额改为引用定义
-- 模型：sys_quota_def(配额字典：quota_key 唯一 + 展示名称)
--       sys_package_quota 由 (package_id, quota_key) 改为 (package_id, quota_id) 引用字典。
-- 全局表（不带 tenant_id），均已在多租户插件 IGNORE_TABLES 中。
-- 约定：quota_value = -1 表示不限；无记录 = 缺省放行（视为不限）。
-- 基座内置定义：max_users（最大用户数）；biz 模块可自助新增定义（建议带模块前缀的 key）。
-- =====================================================================

-- 1) 配额定义字典
CREATE TABLE sys_quota_def (
    id          BIGINT       PRIMARY KEY,
    quota_key   VARCHAR(64)  NOT NULL,
    name        VARCHAR(64)  NOT NULL,
    remark      VARCHAR(255),
    sort        INTEGER      NOT NULL DEFAULT 0,
    created_by  BIGINT,
    created_at  TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_by  BIGINT,
    updated_at  TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted     SMALLINT     NOT NULL DEFAULT 0
);
CREATE UNIQUE INDEX uk_sys_quota_def_key ON sys_quota_def (quota_key) WHERE deleted = 0;
COMMENT ON TABLE sys_quota_def IS '配额定义字典（全局；quota_key 唯一）';
COMMENT ON COLUMN sys_quota_def.quota_key IS '配额标识，如 max_users';
COMMENT ON COLUMN sys_quota_def.name IS '配额展示名称，如 最大用户数';

-- 内置配额定义种子
INSERT INTO sys_quota_def (id, quota_key, name, remark, sort) VALUES
  (1, 'max_users', '最大用户数', '单租户可创建的最大用户数', 1);

-- 2) sys_package_quota 增加 quota_id 引用，回填后弃用 quota_key
ALTER TABLE sys_package_quota ADD COLUMN quota_id BIGINT;
UPDATE sys_package_quota pq
   SET quota_id = d.id
  FROM sys_quota_def d
 WHERE d.quota_key = pq.quota_key;

-- 旧唯一索引（package_id, quota_key）替换为（package_id, quota_id）
DROP INDEX IF EXISTS uk_sys_package_quota;
ALTER TABLE sys_package_quota DROP COLUMN quota_key;
ALTER TABLE sys_package_quota ALTER COLUMN quota_id SET NOT NULL;
CREATE UNIQUE INDEX uk_sys_package_quota ON sys_package_quota (package_id, quota_id) WHERE deleted = 0;
COMMENT ON COLUMN sys_package_quota.quota_id IS '配额定义 id（引用 sys_quota_def）';

-- 3) 菜单：套餐管理新增「配置配额」按钮 perm（仅平台超管，随全功能套餐边界）
INSERT INTO sys_menu (id, parent_id, type, name, path, component, icon, perm, sort, visible) VALUES
  (145, 14, 'F', '配置配额', NULL, NULL, NULL, 'system:package:quota', 5, 1);
INSERT INTO sys_package_menu (id, package_id, menu_id) VALUES (1145, 2, 145);
