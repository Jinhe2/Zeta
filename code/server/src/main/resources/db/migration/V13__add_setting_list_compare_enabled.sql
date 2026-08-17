ALTER TABLE setting_list_item
  ADD COLUMN compare_enabled TINYINT(1) NOT NULL DEFAULT 1
  COMMENT '是否在实验开始前参与定值校核'
  AFTER value_type;
