CREATE TABLE IF NOT EXISTS student_activity_sessions (
  id BIGINT NOT NULL AUTO_INCREMENT,
  user_id BIGINT NOT NULL,
  cabinet_id BIGINT NOT NULL,
  cabinet_name_snapshot VARCHAR(128) NULL,
  device_bind_id VARCHAR(64) NOT NULL,
  bind_label_snapshot VARCHAR(128) NULL,
  started_at TIMESTAMP(6) NOT NULL,
  last_activity_at TIMESTAMP(6) NOT NULL,
  expires_at TIMESTAMP(6) NOT NULL,
  ended_at TIMESTAMP(6) NULL,
  duration_seconds BIGINT NOT NULL,
  status VARCHAR(32) NOT NULL,
  PRIMARY KEY (id),
  KEY idx_sas_user_status (user_id, status),
  KEY idx_sas_cabinet_time (cabinet_id, started_at, expires_at),
  KEY idx_sas_device_status (device_bind_id, status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='学员在线及屏柜活动会话';
