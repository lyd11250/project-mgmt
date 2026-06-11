-- =====================================================================
-- V10: 文件存储（基座系统能力）
-- 模型：sys_file 文件元数据（业务表，带 tenant_id，走多租户插件自动隔离，不入 IGNORE_TABLES）。
--   物理对象存 MinIO（object_key 定位），DB 仅存元数据；删除走软删，物理对象后续由定时任务清理。
-- 配额：新增 max_storage_bytes（单租户最大存储用量，字节；-1=不限），复用 sys_quota_def / QuotaService。
-- 菜单：新增「文件管理」页 + 上传/下载/删除按钮（perm = system:file:*），纳入「全功能」套餐边界。
-- =====================================================================

-- ---------------------------------------------------------------------
-- 文件元数据（业务表，多租户隔离）
-- ---------------------------------------------------------------------
CREATE TABLE sys_file (
    id            BIGINT       PRIMARY KEY,
    tenant_id     BIGINT       NOT NULL,
    original_name VARCHAR(255) NOT NULL,                    -- 原始文件名（展示/下载用）
    object_key    VARCHAR(512) NOT NULL,                    -- 存储层对象键（MinIO 内相对路径）
    storage_type  VARCHAR(32)  NOT NULL DEFAULT 'minio',    -- 存储后端标识，预留多实现
    content_type  VARCHAR(128),                             -- MIME 类型
    size_bytes    BIGINT       NOT NULL,                     -- 字节数
    sha256        VARCHAR(64),                               -- 内容指纹（可选去重/秒传）
    biz_type      VARCHAR(64),                               -- 业务归类（biz 自定义，如 party:attachment）
    created_by    BIGINT,
    created_at    TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_by    BIGINT,
    updated_at    TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted       SMALLINT     NOT NULL DEFAULT 0
);
CREATE INDEX idx_sys_file_tenant_biz ON sys_file (tenant_id, biz_type) WHERE deleted = 0;
COMMENT ON TABLE  sys_file IS '文件元数据（业务表，多租户隔离；物理对象存 MinIO）';
COMMENT ON COLUMN sys_file.object_key IS '存储层对象键（MinIO 桶内相对路径）';
COMMENT ON COLUMN sys_file.biz_type IS '业务归类，由上层模块自定义';

-- ---------------------------------------------------------------------
-- 配额定义：最大存储用量（字节）
-- ---------------------------------------------------------------------
INSERT INTO sys_quota_def (id, quota_key, name, remark, sort) VALUES
  (2, 'max_storage_bytes', '最大存储用量', '单租户文件存储总量上限（字节，-1=不限）', 2);

-- 内置套餐配额：基础版限 1GiB，全功能不限
INSERT INTO sys_package_quota (id, package_id, quota_id, quota_value) VALUES
  (3, 1, 2, 1073741824),
  (4, 2, 2, -1);

-- =====================================================================
-- 种子：文件管理菜单（perm = system:file:*；id 块沿用系统域 1x，取 16）
-- =====================================================================
INSERT INTO sys_menu (id, parent_id, type, name, path, component, icon, perm, sort, visible) VALUES
  (16,  1, 'C', '文件管理', '/system/files', 'system/FileList', 'Folder', 'system:file:list', 6, 1),
  (161, 16, 'F', '上传文件', NULL, NULL, NULL, 'system:file:upload',   1, 1),
  (162, 16, 'F', '下载文件', NULL, NULL, NULL, 'system:file:download', 2, 1),
  (163, 16, 'F', '删除文件', NULL, NULL, NULL, 'system:file:delete',   3, 1);

-- 纳入「全功能」套餐（id=2）；package_menu.id 沿用 1000+menu.id 规约
INSERT INTO sys_package_menu (id, package_id, menu_id)
SELECT 1000 + m.id, 2, m.id FROM sys_menu m
WHERE m.id IN (16, 161, 162, 163);
