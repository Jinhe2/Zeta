CREATE TABLE IF NOT EXISTS experiment_guide_items (
  id BIGINT NOT NULL AUTO_INCREMENT,
  scope_type VARCHAR(32) NOT NULL COMMENT 'LOGIC_DIAGRAM 或 LOGIC_GROUP',
  scope_id BIGINT NOT NULL COMMENT '基础逻辑框图或组合逻辑 ID',
  item_type VARCHAR(32) NOT NULL COMMENT 'IMAGE_TEXT 或 SETTING_LIST',
  title VARCHAR(128) NOT NULL,
  image_url VARCHAR(512) NULL,
  image_data LONGBLOB NULL,
  image_content_type VARCHAR(100) NULL,
  content LONGTEXT NULL COMMENT '文字说明',
  sort_order INT NOT NULL DEFAULT 0,
  enabled TINYINT(1) NOT NULL DEFAULT 1,
  created_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
  PRIMARY KEY (id),
  KEY idx_experiment_guide_scope (scope_type, scope_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='逻辑学习实验引导条目';
