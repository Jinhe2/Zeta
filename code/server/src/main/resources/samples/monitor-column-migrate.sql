-- 手动修复早期 camelCase 列名（可选）
-- 若已启用 BusinessLegacyColumnMigrator 可不必手动执行

ALTER TABLE users CHANGE COLUMN displayName display_name VARCHAR(64) NOT NULL;

-- device_display_items（旧表名 device_cognition_items 会先被自动重命名）
ALTER TABLE device_display_items CHANGE COLUMN sortOrder sort_order INT NOT NULL DEFAULT 0;
ALTER TABLE device_display_items CHANGE COLUMN createdAt created_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6);

-- cabinet_display_items
ALTER TABLE cabinet_display_items CHANGE COLUMN sortOrder sort_order INT NOT NULL DEFAULT 0;
ALTER TABLE cabinet_display_items CHANGE COLUMN createdAt created_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6);

-- 若 created_at 与 createdAt 并存，删除重复列：
-- ALTER TABLE cabinet_display_items DROP COLUMN createdAt;
-- ALTER TABLE device_display_items DROP COLUMN createdAt;
