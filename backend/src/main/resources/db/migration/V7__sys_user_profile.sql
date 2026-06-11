-- =====================================================================
-- V7: sys_user 增加个人资料字段（G12：个人中心 / 自助改资料）
-- 昵称 / 手机号，均可空；基座不强制唯一，避免跨场景冲突。
-- 如上层 biz/* 需要手机唯一，可在各自模块按需追加约束。
-- =====================================================================

ALTER TABLE sys_user ADD COLUMN nickname VARCHAR(64);
ALTER TABLE sys_user ADD COLUMN phone    VARCHAR(32);

COMMENT ON COLUMN sys_user.nickname IS '昵称（展示名）';
COMMENT ON COLUMN sys_user.phone    IS '手机号';
