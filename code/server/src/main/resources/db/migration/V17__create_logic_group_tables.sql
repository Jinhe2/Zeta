CREATE TABLE IF NOT EXISTS logic_group (
  id BIGINT NOT NULL AUTO_INCREMENT,
  ied_device_id BIGINT NOT NULL COMMENT '屏柜库 ied_device.id（跨库引用）',
  name VARCHAR(256) NOT NULL COMMENT '组合逻辑名称',
  sort_order INT NOT NULL DEFAULT 0 COMMENT '装置下组合之间排序',
  created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (id),
  KEY idx_lg_device (ied_device_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='组合逻辑（多个基础逻辑框图拼接）';

CREATE TABLE IF NOT EXISTS logic_group_member (
  id BIGINT NOT NULL AUTO_INCREMENT,
  group_id BIGINT NOT NULL COMMENT 'logic_group.id',
  logic_diagram_id BIGINT NOT NULL COMMENT '屏柜库 logic_diagram.id（跨库引用）',
  sort_order INT NOT NULL DEFAULT 0 COMMENT '成员顺序，决定下发 logic_ids 顺序',
  created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (id),
  UNIQUE KEY uk_lgm_group_logic (group_id, logic_diagram_id),
  KEY idx_lgm_group_order (group_id, sort_order)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='组合逻辑成员（基础逻辑框图）';

CREATE TABLE IF NOT EXISTS logic_group_snapshot (
  id BIGINT NOT NULL AUTO_INCREMENT,
  user_id BIGINT NOT NULL COMMENT 'users.id',
  group_id BIGINT NOT NULL COMMENT 'logic_group.id',
  group_name VARCHAR(256) NULL COMMENT '冗余组合名称，回看展示用',
  snapshot_json JSON NOT NULL COMMENT 'v3.0 组合快照（含 logics[] 各逻辑结果）',
  total_transitions INT NOT NULL DEFAULT 0,
  experiment_passed TINYINT(1) NULL COMMENT '组合整体是否通过',
  status VARCHAR(16) NOT NULL DEFAULT 'COMPLETED',
  source VARCHAR(16) NOT NULL DEFAULT 'MONITOR',
  error_message TEXT NULL,
  created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  completed_at TIMESTAMP NULL DEFAULT NULL,
  PRIMARY KEY (id),
  KEY idx_lgs_user (user_id),
  KEY idx_lgs_group (group_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='组合逻辑实验结果快照';
