-- =====================================================================
-- V9: 业务模块 party（相关方主数据）—— 首个 biz 模块，兼作上层业务接入样板
-- 模型（Fowler Party 模式）：统一主表 party + 子表 party_person / party_organization，
--   相关方按「自然人 vs 组织」二分：PERSON 人员 / ORGANIZATION 组织（单位）。
--   企业只是组织的一种，政府机构/事业单位/社会组织等以 organization.org_type 区分。
--   人员/组织与 party 共享主键（子表 id = party.id），其他模块统一以 party_id 引用相关方。
-- 隔离：三表均带 tenant_id，走多租户插件自动隔离（不入 IGNORE_TABLES）。
-- 唯一性：身份证号 / 统一社会信用代码可空，填了才租户内唯一（偏过滤索引）。
-- =====================================================================

-- ---------------------------------------------------------------------
-- 相关方主表（统一身份：类型 + 显示名）
-- ---------------------------------------------------------------------
CREATE TABLE party (
    id         BIGINT       PRIMARY KEY,
    tenant_id  BIGINT       NOT NULL,
    party_type VARCHAR(16)  NOT NULL,            -- PERSON 人员 / ORGANIZATION 组织
    name       VARCHAR(128) NOT NULL,            -- 统一显示名（人员姓名 / 单位名称）
    status     SMALLINT     NOT NULL DEFAULT 1,  -- 1 启用 0 停用
    remark     VARCHAR(255),
    created_by BIGINT,
    created_at TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_by BIGINT,
    updated_at TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted    SMALLINT     NOT NULL DEFAULT 0
);
CREATE INDEX idx_party_tenant ON party (tenant_id);
CREATE INDEX idx_party_type   ON party (tenant_id, party_type);
COMMENT ON TABLE  party IS '相关方（统一主表，人员/组织的共同身份，供跨模块以 party_id 引用）';
COMMENT ON COLUMN party.party_type IS '相关方类型：PERSON 人员 / ORGANIZATION 组织';
COMMENT ON COLUMN party.name IS '统一显示名（人员姓名 / 单位名称）';

-- ---------------------------------------------------------------------
-- 人员（子表，id = party.id）
-- ---------------------------------------------------------------------
CREATE TABLE party_person (
    id         BIGINT      PRIMARY KEY,          -- = party.id
    tenant_id  BIGINT      NOT NULL,
    gender     SMALLINT    NOT NULL DEFAULT 0,   -- 0 未知 1 男 2 女
    id_card    VARCHAR(32),                      -- 身份证号
    contact    VARCHAR(64),                      -- 联系方式
    created_by BIGINT,
    created_at TIMESTAMP   NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_by BIGINT,
    updated_at TIMESTAMP   NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted    SMALLINT    NOT NULL DEFAULT 0
);
CREATE UNIQUE INDEX uk_party_person_idcard ON party_person (tenant_id, id_card)
    WHERE deleted = 0 AND id_card IS NOT NULL;
COMMENT ON TABLE  party_person IS '人员（party 的 PERSON 子表，共享主键）';
COMMENT ON COLUMN party_person.gender IS '性别：0 未知 1 男 2 女';

-- ---------------------------------------------------------------------
-- 组织/单位（子表，id = party.id）
-- 涵盖企业、政府机构、事业单位、社会组织等，由 org_type 区分。
-- ---------------------------------------------------------------------
CREATE TABLE party_organization (
    id                 BIGINT       PRIMARY KEY, -- = party.id
    tenant_id          BIGINT       NOT NULL,
    org_type           VARCHAR(32),              -- 组织类型（企业/政府机构/事业单位/社会组织/其他）
    tax_no             VARCHAR(32),              -- 统一社会信用代码
    registered_capital VARCHAR(64),             -- 注册资本（自由文本，仅企业适用）
    established_date   DATE,                     -- 成立日期
    legal_person       VARCHAR(64),             -- 法定代表人 / 负责人
    reg_address        VARCHAR(255),            -- 住所（注册地址）
    business_scope     TEXT,                    -- 经营范围（仅企业适用）
    created_by         BIGINT,
    created_at         TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_by         BIGINT,
    updated_at         TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted            SMALLINT     NOT NULL DEFAULT 0
);
CREATE UNIQUE INDEX uk_party_org_taxno ON party_organization (tenant_id, tax_no)
    WHERE deleted = 0 AND tax_no IS NOT NULL;
COMMENT ON TABLE  party_organization IS '组织/单位（party 的 ORGANIZATION 子表，共享主键；企业/政府机构/事业单位等）';
COMMENT ON COLUMN party_organization.org_type IS '组织类型：企业/政府机构/事业单位/社会组织/其他';
COMMENT ON COLUMN party_organization.tax_no IS '统一社会信用代码';

-- =====================================================================
-- 种子：party 模块菜单（perm = 模块:资源:动作；id 块 200，避开系统菜单 1xx）
-- 「人员管理」「单位管理」两个页面；相关方为数据层统一概念，不单列页面。
-- =====================================================================
-- 目录
INSERT INTO sys_menu (id, parent_id, type, name, path, component, icon, perm, sort, visible) VALUES
  (200, 0, 'M', '相关方管理', '/party', NULL, 'Coordinate', NULL, 2, 1);

-- 人员管理（C + 按钮 F）
INSERT INTO sys_menu (id, parent_id, type, name, path, component, icon, perm, sort, visible) VALUES
  (201,  200, 'C', '人员管理', '/party/persons', 'party/PersonList', 'User', 'party:person:list', 1, 1),
  (2011, 201, 'F', '新增人员', NULL, NULL, NULL, 'party:person:create', 1, 1),
  (2012, 201, 'F', '编辑人员', NULL, NULL, NULL, 'party:person:update', 2, 1),
  (2013, 201, 'F', '删除人员', NULL, NULL, NULL, 'party:person:delete', 3, 1);

-- 单位管理（C + 按钮 F）
INSERT INTO sys_menu (id, parent_id, type, name, path, component, icon, perm, sort, visible) VALUES
  (202,  200, 'C', '单位管理', '/party/organizations', 'party/OrganizationList', 'OfficeBuilding', 'party:organization:list', 2, 1),
  (2021, 202, 'F', '新增单位', NULL, NULL, NULL, 'party:organization:create', 1, 1),
  (2022, 202, 'F', '编辑单位', NULL, NULL, NULL, 'party:organization:update', 2, 1),
  (2023, 202, 'F', '删除单位', NULL, NULL, NULL, 'party:organization:delete', 3, 1);

-- =====================================================================
-- 纳入「全功能」套餐（id=2，平台租户）；package_menu.id 沿用 1000+menu.id 规约
-- 基础版（BASIC）不含 party，正好体现「套餐裁剪」边界。
-- =====================================================================
INSERT INTO sys_package_menu (id, package_id, menu_id)
SELECT 1000 + m.id, 2, m.id FROM sys_menu m
WHERE m.id IN (200, 201,2011,2012,2013, 202,2021,2022,2023);
