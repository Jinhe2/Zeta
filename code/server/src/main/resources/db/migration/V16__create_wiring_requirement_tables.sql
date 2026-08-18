CREATE TABLE IF NOT EXISTS wiring_requirement_config (
  id BIGINT NOT NULL AUTO_INCREMENT,
  logic_diagram_id BIGINT NOT NULL COMMENT '屏柜库 logic_diagram.id（跨库引用）',
  category VARCHAR(16) NOT NULL COMMENT 'VOLTAGE 或 CURRENT',
  required TINYINT(1) NOT NULL DEFAULT 0 COMMENT '是否需要接入/校验',
  phase_mode VARCHAR(16) NOT NULL DEFAULT 'THREE_PHASE' COMMENT 'THREE_PHASE 或 SINGLE_PHASE',
  created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (id),
  UNIQUE KEY uk_wiring_req_config_logic_category (logic_diagram_id, category)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='逻辑框图试验仪接线要求配置（电压/电流各一套）';

CREATE TABLE IF NOT EXISTS wiring_requirement_group (
  id BIGINT NOT NULL AUTO_INCREMENT,
  config_id BIGINT NOT NULL COMMENT '所属 wiring_requirement_config.id',
  group_no INT NOT NULL DEFAULT 0 COMMENT '组序号',
  terminal_a_id BIGINT NULL COMMENT 'A相端子 terminal.id',
  terminal_b_id BIGINT NULL COMMENT 'B相端子 terminal.id',
  terminal_c_id BIGINT NULL COMMENT 'C相端子 terminal.id',
  terminal_n_id BIGINT NULL COMMENT 'N相端子 terminal.id',
  created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (id),
  KEY idx_wiring_req_group_config (config_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='试验仪接线要求端子分组（ABCN 一组）';
