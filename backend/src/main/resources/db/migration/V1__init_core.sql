-- =====================================================================
-- V1: 核心鉴权与租户表（第 0 期基线）
-- 约定：主键 BIGINT，由应用层雪花算法生成（MyBatis-Plus ASSIGN_ID）。
-- 除 tenant 外所有表带 tenant_id，由多租户插件自动维护。
-- 审计列 created_by/created_at/updated_by/updated_at，软删除列 deleted(0/1)。
-- =====================================================================

-- 租户（隔离根，不带 tenant_id）
CREATE TABLE tenant (
    id         BIGINT       PRIMARY KEY,
    name       VARCHAR(128) NOT NULL,
    code       VARCHAR(64)  NOT NULL,
    status     SMALLINT     NOT NULL DEFAULT 1,
    expire_at  TIMESTAMP,
    contact    VARCHAR(128),
    created_by BIGINT,
    created_at TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_by BIGINT,
    updated_at TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted    SMALLINT     NOT NULL DEFAULT 0
);
CREATE UNIQUE INDEX uk_tenant_code ON tenant (code) WHERE deleted = 0;
COMMENT ON TABLE tenant IS '租户（系统订阅方，隔离根）';

-- 系统用户
CREATE TABLE sys_user (
    id            BIGINT       PRIMARY KEY,
    tenant_id     BIGINT       NOT NULL,
    username      VARCHAR(64)  NOT NULL,
    password_hash VARCHAR(128) NOT NULL,
    status        SMALLINT     NOT NULL DEFAULT 1,
    person_id     BIGINT,
    created_by    BIGINT,
    created_at    TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_by    BIGINT,
    updated_at    TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted       SMALLINT     NOT NULL DEFAULT 0
);
CREATE UNIQUE INDEX uk_sys_user_tenant_username ON sys_user (tenant_id, username) WHERE deleted = 0;
CREATE INDEX idx_sys_user_tenant ON sys_user (tenant_id);
COMMENT ON TABLE sys_user IS '系统用户（可绑定 person，person 不必有账号）';

-- 角色
CREATE TABLE sys_role (
    id         BIGINT      PRIMARY KEY,
    tenant_id  BIGINT      NOT NULL,
    code       VARCHAR(64) NOT NULL,
    name       VARCHAR(64) NOT NULL,
    created_by BIGINT,
    created_at TIMESTAMP   NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_by BIGINT,
    updated_at TIMESTAMP   NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted    SMALLINT    NOT NULL DEFAULT 0
);
CREATE UNIQUE INDEX uk_sys_role_tenant_code ON sys_role (tenant_id, code) WHERE deleted = 0;
COMMENT ON TABLE sys_role IS '角色';

-- 权限
CREATE TABLE sys_permission (
    id         BIGINT       PRIMARY KEY,
    tenant_id  BIGINT       NOT NULL,
    code       VARCHAR(128) NOT NULL,
    name       VARCHAR(64)  NOT NULL,
    type       VARCHAR(32),
    created_by BIGINT,
    created_at TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_by BIGINT,
    updated_at TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted    SMALLINT     NOT NULL DEFAULT 0
);
CREATE UNIQUE INDEX uk_sys_perm_tenant_code ON sys_permission (tenant_id, code) WHERE deleted = 0;
COMMENT ON TABLE sys_permission IS '权限';

-- 用户-角色关联
CREATE TABLE sys_user_role (
    id         BIGINT    PRIMARY KEY,
    tenant_id  BIGINT    NOT NULL,
    user_id    BIGINT    NOT NULL,
    role_id    BIGINT    NOT NULL,
    created_by BIGINT,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted    SMALLINT  NOT NULL DEFAULT 0
);
CREATE UNIQUE INDEX uk_sys_user_role ON sys_user_role (tenant_id, user_id, role_id) WHERE deleted = 0;
COMMENT ON TABLE sys_user_role IS '用户-角色关联';

-- 角色-权限关联
CREATE TABLE sys_role_permission (
    id            BIGINT    PRIMARY KEY,
    tenant_id     BIGINT    NOT NULL,
    role_id       BIGINT    NOT NULL,
    permission_id BIGINT    NOT NULL,
    created_by    BIGINT,
    created_at    TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted       SMALLINT  NOT NULL DEFAULT 0
);
CREATE UNIQUE INDEX uk_sys_role_perm ON sys_role_permission (tenant_id, role_id, permission_id) WHERE deleted = 0;
COMMENT ON TABLE sys_role_permission IS '角色-权限关联';
