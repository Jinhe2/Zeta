-- 为逻辑框图节点增加认知条目配置

CREATE TABLE IF NOT EXISTS logic_node_cognition_items (
  id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
  logic_diagram_id BIGINT NOT NULL COMMENT 'ct-screen.logic_diagram.id',
  node_id VARCHAR(128) NOT NULL COMMENT '逻辑图节点 ID',
  node_type VARCHAR(32) NOT NULL COMMENT '节点类型：INPUT/TIMER/OUTPUT',
  node_name VARCHAR(256) NOT NULL COMMENT '节点展示名称',
  title VARCHAR(128) NOT NULL COMMENT '认知条目标题',
  image_url VARCHAR(512) NULL COMMENT '旧版图片访问路径',
  image_data LONGBLOB NULL COMMENT '图片二进制数据',
  image_content_type VARCHAR(100) NULL COMMENT '图片 MIME 类型',
  left_percent DOUBLE NULL COMMENT '高亮区域左侧百分比',
  top_percent DOUBLE NULL COMMENT '高亮区域顶部百分比',
  width_percent DOUBLE NULL COMMENT '高亮区域宽度百分比',
  height_percent DOUBLE NULL COMMENT '高亮区域高度百分比',
  content LONGTEXT NOT NULL COMMENT '认知说明文字',
  sort_order INT NOT NULL DEFAULT 0 COMMENT '排序值',
  enabled TINYINT(1) NOT NULL DEFAULT 1 COMMENT '是否启用',
  created_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
  INDEX idx_logic_node_cognition_node (logic_diagram_id, node_id),
  INDEX idx_logic_node_cognition_order (logic_diagram_id, node_id, sort_order, id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='逻辑节点认知条目';
