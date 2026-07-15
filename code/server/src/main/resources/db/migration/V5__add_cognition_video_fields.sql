-- 生产启动时 BusinessLegacyColumnMigrator 会自动、幂等地执行同等迁移；
-- 本文件保留供脱机数据库维护使用。
ALTER TABLE device_display_items
  ADD COLUMN media_type VARCHAR(16) NOT NULL DEFAULT 'IMAGE' COMMENT 'IMAGE / VIDEO',
  ADD COLUMN video_path VARCHAR(512) NULL COMMENT 'JAR 同级视频相对路径';

ALTER TABLE logic_node_cognition_items
  ADD COLUMN media_type VARCHAR(16) NOT NULL DEFAULT 'IMAGE' COMMENT 'IMAGE / VIDEO / TEXT',
  ADD COLUMN video_path VARCHAR(512) NULL COMMENT 'JAR 同级视频相对路径';

UPDATE logic_node_cognition_items
SET media_type = CASE
  WHEN image_url IS NOT NULL OR image_data IS NOT NULL THEN 'IMAGE'
  ELSE 'TEXT'
END;
