CREATE TABLE IF NOT EXISTS hard_pressboard_list_item (
  id BIGINT NOT NULL AUTO_INCREMENT,
  scope_type VARCHAR(32) NOT NULL COMMENT 'IED_DEVICE 或 LOGIC_DIAGRAM',
  scope_id BIGINT NOT NULL COMMENT '屏柜库中的装置或逻辑框图 ID',
  pressboard_ref VARCHAR(512) NOT NULL COMMENT '硬压板台账主键 id（无独立引用字段）',
  pressboard_name VARCHAR(256) NOT NULL,
  baseline_value TINYINT(1) NOT NULL COMMENT '1=投入，0=退出',
  compare_enabled TINYINT(1) NOT NULL DEFAULT 1,
  sort_order INT NOT NULL DEFAULT 0,
  created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (id),
  UNIQUE KEY uk_hard_pressboard_list_scope_ref (scope_type, scope_id, pressboard_ref),
  KEY idx_hard_pressboard_list_scope (scope_type, scope_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='逻辑学习硬压板基准清单项';
