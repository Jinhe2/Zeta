CREATE TABLE IF NOT EXISTS sampling_test_items (
  id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  screen_cabinet_id BIGINT UNSIGNED NOT NULL COMMENT 'ct-screen.cabinet.id',
  title VARCHAR(128) NOT NULL,
  media_type VARCHAR(32) NOT NULL COMMENT 'IMAGE / VIDEO / SAMPLING_CONFIGURATION',
  image_url VARCHAR(512) NULL,
  image_data LONGBLOB NULL,
  image_content_type VARCHAR(100) NULL,
  video_path VARCHAR(512) NULL,
  content LONGTEXT NOT NULL,
  sort_order INT NOT NULL DEFAULT 0,
  enabled TINYINT(1) NOT NULL DEFAULT 1,
  created_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
  PRIMARY KEY (id),
  INDEX idx_sampling_test_cabinet (screen_cabinet_id, sort_order, id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='采样测试认知条目';

CREATE TABLE IF NOT EXISTS sampling_test_channels (
  id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  sampling_test_item_id BIGINT UNSIGNED NOT NULL,
  output_code VARCHAR(2) NOT NULL COMMENT 'Ua/Ub/Uc/Un/Ia/Ib/Ic/In',
  terminal_id BIGINT UNSIGNED NOT NULL COMMENT 'ct-screen.terminal.id',
  baseline_magnitude DECIMAL(18,6) NULL,
  baseline_angle DECIMAL(12,6) NULL,
  sort_order INT NOT NULL,
  PRIMARY KEY (id),
  UNIQUE KEY uk_sampling_channel_code (sampling_test_item_id, output_code),
  UNIQUE KEY uk_sampling_channel_terminal (sampling_test_item_id, terminal_id),
  INDEX idx_sampling_channel_item (sampling_test_item_id, sort_order, id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='采样测试八路通道配置';
