ALTER TABLE device_display_items
  MODIFY COLUMN media_type VARCHAR(32) NOT NULL DEFAULT 'IMAGE' COMMENT 'IMAGE / VIDEO / TERMINAL_OPERATION / IED_BASELINE_SETTING';

CREATE TABLE IF NOT EXISTS terminal_operation_items (
  id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  device_display_item_id BIGINT UNSIGNED NOT NULL,
  terminal_strip_id BIGINT UNSIGNED NOT NULL,
  PRIMARY KEY (id),
  UNIQUE KEY uk_terminal_operation_item (device_display_item_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='端子操作认知条目';

CREATE TABLE IF NOT EXISTS terminal_operation_terminals (
  id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  terminal_operation_id BIGINT UNSIGNED NOT NULL,
  terminal_id BIGINT UNSIGNED NOT NULL,
  terminal_meaning VARCHAR(128) NOT NULL,
  sort_order INT NOT NULL,
  PRIMARY KEY (id),
  INDEX idx_terminal_operation_terminal (terminal_operation_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='端子操作目标端子';

UPDATE device_display_items d
INNER JOIN terminal_operation_items o ON o.device_display_item_id = d.id
SET d.media_type = 'TERMINAL_OPERATION'
WHERE d.media_type <> 'TERMINAL_OPERATION';
