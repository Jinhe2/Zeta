-- 学员资源库资料。screen_cabinet_id 为 NULL 时表示所有屏柜可见。
CREATE TABLE IF NOT EXISTS learning_resources (
  id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  name VARCHAR(128) NOT NULL,
  description LONGTEXT NOT NULL,
  resource_type VARCHAR(32) NOT NULL COMMENT 'DEBUG_OUTLINE / MANUAL / DRAWING / VIDEO_COURSE',
  screen_cabinet_id BIGINT UNSIGNED NULL COMMENT 'ct-screen.cabinet.id；NULL 表示所有屏柜',
  file_path VARCHAR(512) NOT NULL COMMENT 'JAR 同级 resource/pdf 或 resource/video 相对路径',
  original_filename VARCHAR(255) NOT NULL,
  content_type VARCHAR(100) NOT NULL,
  file_size BIGINT NOT NULL,
  created_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
  updated_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
  PRIMARY KEY (id),
  INDEX idx_learning_resources_scope (resource_type, screen_cabinet_id, updated_at, id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='学员学习资料';
