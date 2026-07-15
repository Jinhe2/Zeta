CREATE TABLE IF NOT EXISTS logic_learning_configs (
  id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  logic_diagram_id BIGINT UNSIGNED NOT NULL COMMENT 'ct-screen.logic_diagram.id',
  sort_order INT NOT NULL DEFAULT 0 COMMENT '学员端逻辑框图显示顺序',
  PRIMARY KEY (id),
  UNIQUE KEY uk_logic_learning_config_diagram (logic_diagram_id),
  INDEX idx_logic_learning_config_order (sort_order, logic_diagram_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='逻辑学习展示配置';
