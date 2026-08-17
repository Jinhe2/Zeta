CREATE TABLE IF NOT EXISTS setting_list_item (
  id BIGINT NOT NULL AUTO_INCREMENT,
  scope_type VARCHAR(32) NOT NULL COMMENT 'IED_DEVICE 或 LOGIC_DIAGRAM',
  scope_id BIGINT NOT NULL COMMENT '屏柜库中的装置或逻辑框图 ID',
  setting_ref VARCHAR(512) NOT NULL,
  setting_fc VARCHAR(16) NOT NULL DEFAULT 'SG',
  setting_name VARCHAR(256) NOT NULL,
  value_type VARCHAR(16) NOT NULL,
  baseline_value VARCHAR(64) NOT NULL,
  sort_order INT NOT NULL DEFAULT 0,
  created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (id),
  UNIQUE KEY uk_setting_list_scope_ref (scope_type, scope_id, setting_ref),
  KEY idx_setting_list_scope (scope_type, scope_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='逻辑学习定值清单项';
