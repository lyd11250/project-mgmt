-- =====================================================================
-- V8: sys_menu 增加 keep_alive 字段（页签缓存：前端 keep-alive）
-- 仅 C 型页面有意义；默认 0（不缓存），按需在菜单管理页逐个开启。
-- =====================================================================

ALTER TABLE sys_menu ADD COLUMN keep_alive SMALLINT NOT NULL DEFAULT 0;

COMMENT ON COLUMN sys_menu.keep_alive IS '页面是否启用前端缓存(keep-alive)：1 是 0 否';
