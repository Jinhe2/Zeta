-- 试验仪接线要求由「绑定逻辑框图」泛化为「scope_type + scope_id」，
-- 以同时支持基础逻辑（LOGIC_DIAGRAM）与组合逻辑（LOGIC_GROUP）。

-- 1. 新增 scope 列（带默认值，供回填）
ALTER TABLE wiring_requirement_config
  ADD COLUMN scope_type VARCHAR(32) NOT NULL DEFAULT 'LOGIC_DIAGRAM' COMMENT 'LOGIC_DIAGRAM 或 LOGIC_GROUP' AFTER id,
  ADD COLUMN scope_id BIGINT NOT NULL DEFAULT 0 COMMENT '屏柜库逻辑框图 ID 或业务库组合 ID' AFTER scope_type;

-- 2. 回填旧数据：原有接线均绑定逻辑框图
UPDATE wiring_requirement_config SET scope_type = 'LOGIC_DIAGRAM', scope_id = logic_diagram_id;

-- 3. 删除旧唯一键与旧列
ALTER TABLE wiring_requirement_config DROP INDEX uk_wiring_req_config_logic_category;
ALTER TABLE wiring_requirement_config DROP COLUMN logic_diagram_id;

-- 4. 建立新唯一键（scope_type + scope_id + category）
ALTER TABLE wiring_requirement_config ADD UNIQUE KEY uk_wiring_req_config_scope_category (scope_type, scope_id, category);
