-- =====================================================================
-- V4: 套餐配额（键值模型）
-- 模型：套餐 sys_package ─< sys_package_quota(quota_key, quota_value)
-- 全局表（不带 tenant_id），已在多租户插件 IGNORE_TABLES 中。
-- 约定：quota_value = -1 表示不限；无记录 = 缺省放行（视为不限）。
-- 基座内置 quota_key：max_users（单租户最大用户数）；biz 模块可自助新增 key。
-- =====================================================================

CREATE TABLE sys_package_quota (
    id          BIGINT       PRIMARY KEY,
    package_id  BIGINT       NOT NULL,
    quota_key   VARCHAR(64)  NOT NULL,
    quota_value BIGINT       NOT NULL DEFAULT -1,
    created_by  BIGINT,
    created_at  TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_by  BIGINT,
    updated_at  TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted     SMALLINT     NOT NULL DEFAULT 0
);
CREATE UNIQUE INDEX uk_sys_package_quota ON sys_package_quota (package_id, quota_key) WHERE deleted = 0;
COMMENT ON TABLE sys_package_quota IS '套餐配额（全局，键值模型；-1=不限）';
COMMENT ON COLUMN sys_package_quota.quota_key IS '配额标识，如 max_users';
COMMENT ON COLUMN sys_package_quota.quota_value IS '配额上限，-1 表示不限';

-- 内置套餐配额种子：基础版限 10 用户，全功能不限
INSERT INTO sys_package_quota (id, package_id, quota_key, quota_value) VALUES
  (1, 1, 'max_users', 10),
  (2, 2, 'max_users', -1);
