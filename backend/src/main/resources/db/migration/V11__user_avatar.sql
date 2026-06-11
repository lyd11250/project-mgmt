-- =====================================================================
-- V11: 用户头像
-- sys_user 增加 avatar_file_id，指向 sys_file.id（当前头像文件）。
-- 头像物理对象与元数据复用文件能力（sys_file，计入存储配额）；换头像时旧记录软删。
-- 不加外键约束（与全库一致，关系在应用层维护），可空 = 未设置头像。
-- =====================================================================
ALTER TABLE sys_user ADD COLUMN avatar_file_id BIGINT;
COMMENT ON COLUMN sys_user.avatar_file_id IS '当前头像文件 id（引用 sys_file；空=未设置）';
