ALTER TABLE sampling_test_items
  ADD COLUMN ied_device_id BIGINT UNSIGNED NULL AFTER screen_cabinet_id;

CREATE TABLE IF NOT EXISTS digital_sampling_test_channels (
  id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  sampling_test_item_id BIGINT UNSIGNED NOT NULL,
  sampling_signal_association_id BIGINT UNSIGNED NOT NULL,
  sampling_signal_channel_id BIGINT UNSIGNED NOT NULL,
  ied_device_id BIGINT UNSIGNED NOT NULL,
  category VARCHAR(16) NOT NULL,
  control_identity_key CHAR(64) NOT NULL,
  source_ied_name VARCHAR(128) NULL,
  control_name VARCHAR(128) NULL,
  control_reference VARCHAR(512) NULL,
  telemetry_reference VARCHAR(768) NOT NULL,
  telemetry_key CHAR(64) NOT NULL,
  telemetry_description VARCHAR(512) NULL,
  dataset_name VARCHAR(128) NULL,
  value_type VARCHAR(64) NULL,
  baseline_magnitude DECIMAL(18,6) NOT NULL,
  baseline_angle DECIMAL(12,6) NOT NULL,
  sort_order INT NOT NULL,
  PRIMARY KEY (id),
  UNIQUE KEY uk_digital_sampling_channel (sampling_test_item_id, sampling_signal_channel_id),
  UNIQUE KEY uk_digital_sampling_ref (sampling_test_item_id, telemetry_key),
  INDEX idx_digital_sampling_item (sampling_test_item_id, sort_order, id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='数字化采样测试通道快照';
