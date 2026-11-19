CREATE TABLE IF NOT EXISTS learning_modules (
  code VARCHAR(32) NOT NULL,
  name VARCHAR(64) NOT NULL,
  description VARCHAR(256) NOT NULL,
  sort_order INT NOT NULL,
  enabled TINYINT(1) NOT NULL DEFAULT 1,
  completion_mode VARCHAR(32) NOT NULL DEFAULT 'MANUAL_CONFIRM',
  PRIMARY KEY (code)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='教练模式学习模块';

CREATE TABLE IF NOT EXISTS learning_module_prerequisites (
  id BIGINT NOT NULL AUTO_INCREMENT,
  module_code VARCHAR(32) NOT NULL,
  prerequisite_code VARCHAR(32) NOT NULL,
  PRIMARY KEY (id),
  UNIQUE KEY uk_learning_module_prerequisite (module_code, prerequisite_code),
  KEY idx_learning_prerequisite_module (module_code)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='学习模块前置关系';

CREATE TABLE IF NOT EXISTS student_learning_progress (
  id BIGINT NOT NULL AUTO_INCREMENT,
  user_id BIGINT NOT NULL,
  module_code VARCHAR(32) NOT NULL,
  status VARCHAR(24) NOT NULL,
  unlocked_at TIMESTAMP(6) NOT NULL,
  started_at TIMESTAMP(6) NULL,
  completed_at TIMESTAMP(6) NULL,
  last_activity_at TIMESTAMP(6) NULL,
  visit_count INT NOT NULL DEFAULT 0,
  PRIMARY KEY (id),
  UNIQUE KEY uk_student_learning_progress (user_id, module_code),
  KEY idx_student_learning_progress_user (user_id, status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='学员全局学习解锁进度';

CREATE TABLE IF NOT EXISTS learning_progress_events (
  id BIGINT NOT NULL AUTO_INCREMENT,
  user_id BIGINT NOT NULL,
  module_code VARCHAR(32) NOT NULL,
  event_type VARCHAR(32) NOT NULL,
  operator_user_id BIGINT NULL,
  occurred_at TIMESTAMP(6) NOT NULL,
  note VARCHAR(256) NULL,
  PRIMARY KEY (id),
  KEY idx_learning_progress_event_user (user_id, occurred_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='学习进度变更记录';

INSERT IGNORE INTO learning_modules
  (code, name, description, sort_order, enabled, completion_mode)
VALUES
  ('CABINET', '屏柜学习', '认识当前屏柜、装置与操作区域', 10, 1, 'MANUAL_CONFIRM'),
  ('DRAWING', '图纸学习', '结合图纸理解屏柜与回路关系', 20, 1, 'MANUAL_CONFIRM'),
  ('CIRCUIT', '回路学习', '学习实体回路或虚回路连接关系', 30, 1, 'MANUAL_CONFIRM'),
  ('SAMPLING', '采样测试', '学习采样通道并完成测试训练', 40, 1, 'MANUAL_CONFIRM'),
  ('LOGIC', '逻辑原理', '理解保护逻辑与动作过程', 50, 1, 'MANUAL_CONFIRM');

INSERT IGNORE INTO learning_module_prerequisites (module_code, prerequisite_code)
VALUES
  ('DRAWING', 'CABINET'),
  ('CIRCUIT', 'DRAWING'),
  ('SAMPLING', 'CIRCUIT'),
  ('LOGIC', 'SAMPLING');
