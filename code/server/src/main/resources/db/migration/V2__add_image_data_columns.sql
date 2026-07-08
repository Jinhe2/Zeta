-- 静态文件数据库存储迁移
-- 将图片二进制数据存入数据库，移除文件系统依赖
-- 执行前请备份数据库

-- cabinet_display_items 表
ALTER TABLE cabinet_display_items
  ADD COLUMN image_data LONGBLOB COMMENT '图片二进制数据',
  ADD COLUMN image_content_type VARCHAR(100) COMMENT 'MIME类型',
  MODIFY COLUMN image_url VARCHAR(512) NULL COMMENT '兼容旧数据';

-- device_display_items 表
ALTER TABLE device_display_items
  ADD COLUMN image_data LONGBLOB COMMENT '图片二进制数据',
  ADD COLUMN image_content_type VARCHAR(100) COMMENT 'MIME类型',
  MODIFY COLUMN image_url VARCHAR(512) NULL COMMENT '兼容旧数据';
