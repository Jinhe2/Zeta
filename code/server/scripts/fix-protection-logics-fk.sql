-- 知识结构升级后，若启动报 device_id 外键错误，可在 MySQL 中执行本脚本后重启服务。
-- 用法：mysql -u用户 -p 数据库名 < scripts/fix-protection-logics-fk.sql

SET FOREIGN_KEY_CHECKS = 0;

-- 删除 Hibernate 可能已创建失败的外键（名称以实际库中为准，可先 SHOW CREATE TABLE protection_logics; 查看）
SET @fk_name = (
    SELECT CONSTRAINT_NAME
    FROM information_schema.KEY_COLUMN_USAGE
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'protection_logics'
      AND COLUMN_NAME = 'device_id'
      AND REFERENCED_TABLE_NAME = 'devices'
    LIMIT 1
);
SET @drop_fk = IF(@fk_name IS NOT NULL, CONCAT('ALTER TABLE protection_logics DROP FOREIGN KEY ', @fk_name), 'SELECT 1');
PREPARE stmt FROM @drop_fk;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- 允许 device_id 为空，便于应用启动后由 DataInitializer 自动补全关联
ALTER TABLE protection_logics MODIFY COLUMN device_id BIGINT NULL;

SET FOREIGN_KEY_CHECKS = 1;
