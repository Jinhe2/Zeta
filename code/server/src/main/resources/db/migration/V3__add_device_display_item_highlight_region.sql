-- 为子设备认知条目图片增加可选高亮区域

ALTER TABLE device_display_items
  ADD COLUMN left_percent DOUBLE NULL COMMENT '高亮区域左侧百分比',
  ADD COLUMN top_percent DOUBLE NULL COMMENT '高亮区域顶部百分比',
  ADD COLUMN width_percent DOUBLE NULL COMMENT '高亮区域宽度百分比',
  ADD COLUMN height_percent DOUBLE NULL COMMENT '高亮区域高度百分比';
