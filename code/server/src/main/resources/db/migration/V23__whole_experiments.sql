CREATE TABLE IF NOT EXISTS whole_experiment (
  id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
  user_id BIGINT NOT NULL,
  device_id BIGINT NOT NULL,
  member_signature VARCHAR(128) NOT NULL,
  last_started_at DATETIME(6) NULL,
  created_at DATETIME(6) NOT NULL,
  UNIQUE KEY uk_whole_experiment_owner (user_id, device_id, member_signature),
  KEY idx_whole_experiment_recent (user_id, device_id, last_started_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS whole_experiment_member (
  id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
  experiment_id BIGINT NOT NULL,
  logic_diagram_id BIGINT NOT NULL,
  sequence_no INT NOT NULL,
  logic_code VARCHAR(256) NULL,
  logic_title VARCHAR(512) NULL,
  UNIQUE KEY uk_whole_experiment_sequence (experiment_id, sequence_no),
  UNIQUE KEY uk_whole_experiment_logic (experiment_id, logic_diagram_id),
  CONSTRAINT fk_whole_member_experiment FOREIGN KEY (experiment_id) REFERENCES whole_experiment(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS whole_experiment_run (
  task_uuid VARCHAR(36) NOT NULL PRIMARY KEY,
  experiment_id BIGINT NOT NULL,
  user_id BIGINT NOT NULL,
  members_json LONGTEXT NOT NULL,
  ied_name VARCHAR(256) NOT NULL,
  status VARCHAR(32) NOT NULL,
  result_json LONGTEXT NULL,
  snapshot_json LONGTEXT NULL,
  total_transitions INT DEFAULT 0,
  experiment_passed TINYINT(1) NULL,
  error_message LONGTEXT NULL,
  created_at DATETIME(6) NOT NULL,
  accepted_at DATETIME(6) NULL,
  completed_at DATETIME(6) NULL,
  KEY idx_whole_run_history (experiment_id, user_id, created_at),
  CONSTRAINT fk_whole_run_experiment FOREIGN KEY (experiment_id) REFERENCES whole_experiment(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
