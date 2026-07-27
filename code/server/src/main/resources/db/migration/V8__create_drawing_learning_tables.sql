CREATE TABLE IF NOT EXISTS drawing_groups (
  id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  screen_cabinet_id BIGINT UNSIGNED NOT NULL COMMENT 'ct-screen.cabinet.id',
  drawing_type VARCHAR(32) NOT NULL COMMENT 'BLUEPRINT / WHITEPRINT',
  name VARCHAR(128) NOT NULL,
  sort_order INT NOT NULL DEFAULT 0,
  enabled TINYINT(1) NOT NULL DEFAULT 1,
  created_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
  PRIMARY KEY (id),
  INDEX idx_drawing_groups_cabinet (screen_cabinet_id, drawing_type, sort_order, id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='图纸学习分组';

CREATE TABLE IF NOT EXISTS drawing_pages (
  id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  drawing_group_id BIGINT UNSIGNED NOT NULL,
  title VARCHAR(128) NOT NULL,
  image_url VARCHAR(512) NULL COMMENT '兼容旧图片路径',
  image_data LONGBLOB NULL,
  image_content_type VARCHAR(100) NULL,
  sort_order INT NOT NULL DEFAULT 0,
  enabled TINYINT(1) NOT NULL DEFAULT 1,
  created_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
  PRIMARY KEY (id),
  INDEX idx_drawing_pages_group (drawing_group_id, sort_order, id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='图纸学习图纸';

CREATE TABLE IF NOT EXISTS drawing_cognition_items (
  id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  drawing_page_id BIGINT UNSIGNED NOT NULL,
  title VARCHAR(128) NOT NULL,
  content LONGTEXT NOT NULL,
  left_percent DOUBLE NULL,
  top_percent DOUBLE NULL,
  width_percent DOUBLE NULL,
  height_percent DOUBLE NULL,
  sort_order INT NOT NULL DEFAULT 0,
  enabled TINYINT(1) NOT NULL DEFAULT 1,
  created_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
  PRIMARY KEY (id),
  INDEX idx_drawing_cognition_page (drawing_page_id, sort_order, id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='图纸学习认知条目';
