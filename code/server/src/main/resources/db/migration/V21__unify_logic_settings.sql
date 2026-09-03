CREATE TABLE IF NOT EXISTS logic_setting_selection (
  id BIGINT NOT NULL AUTO_INCREMENT,
  scope_type VARCHAR(32) NOT NULL COMMENT 'LOGIC_DIAGRAM 或 LOGIC_GROUP',
  scope_id BIGINT NOT NULL,
  setting_ref VARCHAR(512) NOT NULL,
  PRIMARY KEY (id),
  UNIQUE KEY uk_logic_setting_selection (scope_type, scope_id, setting_ref)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='逻辑校验项目选择';

CREATE TABLE IF NOT EXISTS business_migration_state (
  migration_key VARCHAR(128) NOT NULL,
  completed TINYINT(1) NOT NULL DEFAULT 0,
  PRIMARY KEY (migration_key)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='业务数据一次性迁移状态';

-- 已由启动迁移添加这些列的环境不再手动执行以下 ALTER。
ALTER TABLE experiment_guide_items
  ADD COLUMN show_in_whole_experiment TINYINT(1) NOT NULL DEFAULT 1 COMMENT '在整组实验中显示';
ALTER TABLE logic_learning_configs
  ADD COLUMN whole_experiment_sequence INT NOT NULL DEFAULT 1 COMMENT '整组试验序列，1 至 3';
-- 历史勾选需要读取屏柜库映射，由应用启动时执行一次性迁移。
