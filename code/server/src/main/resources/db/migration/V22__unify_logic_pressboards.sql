CREATE TABLE IF NOT EXISTS logic_pressboard_selection (
  id BIGINT NOT NULL AUTO_INCREMENT,
  pressboard_kind VARCHAR(16) NOT NULL COMMENT 'SOFT 或 HARD',
  scope_type VARCHAR(32) NOT NULL COMMENT 'LOGIC_DIAGRAM 或 LOGIC_GROUP',
  scope_id BIGINT NOT NULL,
  pressboard_ref VARCHAR(512) NOT NULL,
  PRIMARY KEY (id),
  UNIQUE KEY uk_logic_pressboard_selection (pressboard_kind, scope_type, scope_id, pressboard_ref)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='逻辑压板校验项目选择';
-- 历史勾选由应用启动迁移一次性转换，使用 business_migration_state 独立记录 V22 状态。
