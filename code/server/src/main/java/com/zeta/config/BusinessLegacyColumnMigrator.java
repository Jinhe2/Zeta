package com.zeta.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import javax.sql.DataSource;

/**
 * 修复早期 Hibernate camelCase 列名与当前 snake_case 实体不一致的问题；
 * 并将 cognition 表重命名为 display 表。
 */
@Component
@Order(15)
public class BusinessLegacyColumnMigrator implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(BusinessLegacyColumnMigrator.class);

    private final JdbcTemplate jdbcTemplate;

    public BusinessLegacyColumnMigrator(@Qualifier("businessDataSource") DataSource businessDataSource) {
        this.jdbcTemplate = new JdbcTemplate(businessDataSource);
    }

    @Override
    public void run(String... args) {
        migrateUsers();
        renameTableIfNeeded("device_cognition_items", "device_display_items");
        renameTableIfNeeded("cabinet_cognition_items", "cabinet_display_items");
        migrateDisplayTable("device_display_items");
        migrateDisplayTable("cabinet_display_items");
        migrateCabinetDisplayImageUrl();
        migrateDeviceDisplayImageUrl();
        migrateCognitionDevices();
        migrateCognitionMediaFields();
    }

    private void migrateUsers() {
        if (!tableExists("users")) {
            return;
        }
        renameColumnIfExists("users", "displayName", "display_name", "VARCHAR(64) NOT NULL");
        fixTimestampColumn("users");
    }

    private void renameTableIfNeeded(String oldName, String newName) {
        if (tableExists(newName) || !tableExists(oldName)) {
            return;
        }
        String sql = "RENAME TABLE `" + oldName + "` TO `" + newName + "`";
        if (executeIgnoreError(sql)) {
            log.info("业务库表重命名：{} → {}", oldName, newName);
        }
    }

    private void migrateDeviceDisplayImageUrl() {
        String table = "device_display_items";
        if (!tableExists(table) || columnExists(table, "image_url")) {
            return;
        }
        String sql = "ALTER TABLE `" + table + "` ADD COLUMN `image_url` VARCHAR(512) NOT NULL "
                + "DEFAULT '/images/protection-device.svg' COMMENT '认知图片路径' AFTER `title`";
        if (executeIgnoreError(sql)) {
            log.info("业务库 {}：新增列 image_url", table);
        }
    }

    private void migrateCognitionDevices() {
        createCognitionDevicesTable();
        migrateLegacyDeviceCabinetRegions();
        migrateDeviceDisplayItemsCognitionDeviceId();
    }

    /**
     * 视频认知字段迁移。
     *
     * Hibernate 的 ddl-auto:update 会依据 nullable=false 添加 NOT NULL 列，但不会把
     * Java 字段的初始值写成数据库 DEFAULT；MariaDB 会将旧行填为 ''，随后枚举读取失败。
     * 因此这里始终先以可空列补齐，再回填并收紧为 NOT NULL DEFAULT。
     */
    private void migrateCognitionMediaFields() {
        migrateDeviceDisplayMediaFields();
        migrateLogicNodeMediaFields();
    }

    private void migrateDeviceDisplayMediaFields() {
        String table = "device_display_items";
        if (!tableExists(table)) {
            return;
        }
        addColumnIfMissing(table, "media_type", "VARCHAR(16) NULL COMMENT 'IMAGE / VIDEO'");
        addColumnIfMissing(table, "video_path", "VARCHAR(512) NULL COMMENT 'JAR 同级视频相对路径'");
        executeRequired("UPDATE `" + table + "` SET `media_type` = 'IMAGE' "
                + "WHERE `media_type` IS NULL OR TRIM(`media_type`) = '' "
                + "OR `media_type` NOT IN ('IMAGE', 'VIDEO')");
        executeRequired("ALTER TABLE `" + table + "` MODIFY COLUMN `media_type` "
                + "VARCHAR(16) NOT NULL DEFAULT 'IMAGE' COMMENT 'IMAGE / VIDEO'");
        log.info("业务库 {}：认知媒体字段迁移完成", table);
    }

    private void migrateLogicNodeMediaFields() {
        String table = "logic_node_cognition_items";
        if (!tableExists(table)) {
            return;
        }
        addColumnIfMissing(table, "media_type", "VARCHAR(16) NULL COMMENT 'IMAGE / VIDEO / TEXT'");
        addColumnIfMissing(table, "video_path", "VARCHAR(512) NULL COMMENT 'JAR 同级视频相对路径'");
        executeRequired("UPDATE `" + table + "` SET `media_type` = CASE "
                + "WHEN `image_url` IS NOT NULL OR `image_data` IS NOT NULL THEN 'IMAGE' "
                + "ELSE 'TEXT' END "
                + "WHERE `media_type` IS NULL OR TRIM(`media_type`) = '' "
                + "OR `media_type` NOT IN ('IMAGE', 'VIDEO', 'TEXT')");
        executeRequired("ALTER TABLE `" + table + "` MODIFY COLUMN `media_type` "
                + "VARCHAR(16) NOT NULL DEFAULT 'IMAGE' COMMENT 'IMAGE / VIDEO / TEXT'");
        log.info("业务库 {}：认知媒体字段迁移完成", table);
    }

    private void addColumnIfMissing(String table, String column, String definition) {
        if (columnExists(table, column)) {
            return;
        }
        executeRequired("ALTER TABLE `" + table + "` ADD COLUMN `" + column + "` " + definition);
        log.info("业务库 {}：新增列 {}", table, column);
    }

    private void executeRequired(String sql) {
        try {
            jdbcTemplate.execute(sql);
        } catch (Exception ex) {
            throw new IllegalStateException("业务库认知视频字段迁移失败: " + sql, ex);
        }
    }

    private void createCognitionDevicesTable() {
        if (tableExists("cognition_devices")) {
            return;
        }
        String sql = "CREATE TABLE IF NOT EXISTS cognition_devices ("
                + "id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT, "
                + "cabinet_display_item_id BIGINT UNSIGNED NOT NULL, "
                + "device_type VARCHAR(32) NOT NULL, "
                + "screen_device_id BIGINT UNSIGNED NULL, "
                + "title VARCHAR(128) NOT NULL, "
                + "left_percent DOUBLE NOT NULL, "
                + "top_percent DOUBLE NOT NULL, "
                + "width_percent DOUBLE NOT NULL, "
                + "height_percent DOUBLE NOT NULL, "
                + "sort_order INT NOT NULL DEFAULT 0, "
                + "enabled TINYINT(1) NOT NULL DEFAULT 1, "
                + "created_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6), "
                + "PRIMARY KEY (id), "
                + "INDEX idx_cd_cabinet_item (cabinet_display_item_id)"
                + ") ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci "
                + "COMMENT='屏柜学习图上的抽象设备'";
        if (executeIgnoreError(sql)) {
            log.info("业务库：创建表 cognition_devices");
        }
    }

    private void migrateLegacyDeviceCabinetRegions() {
        if (!tableExists("device_cabinet_regions") || !tableExists("cognition_devices")) {
            return;
        }
        String sql = "INSERT INTO cognition_devices "
                + "(cabinet_display_item_id, device_type, screen_device_id, title, "
                + "left_percent, top_percent, width_percent, height_percent, sort_order, enabled) "
                + "SELECT r.cabinet_display_item_id, 'IED', r.screen_device_id, "
                + "CONCAT('IED-', r.screen_device_id), r.x_percent, r.y_percent, "
                + "r.width_percent, r.height_percent, 0, 1 "
                + "FROM device_cabinet_regions r "
                + "WHERE NOT EXISTS ("
                + "  SELECT 1 FROM cognition_devices c "
                + "  WHERE c.screen_device_id = r.screen_device_id "
                + "  AND c.cabinet_display_item_id = r.cabinet_display_item_id"
                + ")";
        if (executeIgnoreError(sql)) {
            log.info("业务库：从 device_cabinet_regions 迁移至 cognition_devices");
        }
        executeIgnoreError("DROP TABLE IF EXISTS device_cabinet_regions");
    }

    private void migrateDeviceDisplayItemsCognitionDeviceId() {
        String table = "device_display_items";
        if (!tableExists(table)) {
            return;
        }
        if (!columnExists(table, "cognition_device_id")) {
            String addCol = "ALTER TABLE `" + table + "` ADD COLUMN `cognition_device_id` "
                    + "BIGINT UNSIGNED NULL COMMENT '认知设备 id' AFTER `id`";
            executeIgnoreError(addCol);
            log.info("业务库 {}：新增列 cognition_device_id", table);
        }
        if (columnExists(table, "screen_device_id") && columnExists(table, "cognition_device_id")) {
            String backfill = "UPDATE `" + table + "` ddi "
                    + "INNER JOIN cognition_devices cd ON cd.screen_device_id = ddi.screen_device_id "
                    + "SET ddi.cognition_device_id = cd.id "
                    + "WHERE ddi.cognition_device_id IS NULL";
            executeIgnoreError(backfill);
            log.info("业务库 {}：回填 cognition_device_id", table);
            executeIgnoreError("ALTER TABLE `" + table + "` DROP COLUMN `screen_device_id`");
            log.info("业务库 {}：删除列 screen_device_id", table);
        }
        if (columnExists(table, "cognition_device_id")) {
            executeIgnoreError("ALTER TABLE `" + table + "` MODIFY COLUMN `cognition_device_id` "
                    + "BIGINT UNSIGNED NOT NULL");
        }
    }

    private void migrateCabinetDisplayImageUrl() {
        String table = "cabinet_display_items";
        if (!tableExists(table) || columnExists(table, "image_url")) {
            return;
        }
        String sql = "ALTER TABLE `" + table + "` ADD COLUMN `image_url` VARCHAR(512) NOT NULL "
                + "DEFAULT '/images/cabinet-structure.svg' COMMENT '认知图片路径' AFTER `title`";
        if (executeIgnoreError(sql)) {
            log.info("业务库 {}：新增列 image_url", table);
        }
    }

    private void migrateDisplayTable(String table) {
        if (!tableExists(table)) {
            return;
        }
        renameColumnIfExists(table, "sortOrder", "sort_order", "INT NOT NULL DEFAULT 0");
        fixTimestampColumn(table);
    }

    private void fixTimestampColumn(String table) {
        boolean hasLegacy = columnExists(table, "createdAt");
        boolean hasSnake = columnExists(table, "created_at");
        if (hasLegacy && hasSnake) {
            if (executeIgnoreError("ALTER TABLE `" + table + "` DROP COLUMN `createdAt`")) {
                log.info("业务库 {}：删除重复列 createdAt", table);
            }
            return;
        }
        if (hasLegacy) {
            String sql = "ALTER TABLE `" + table + "` CHANGE COLUMN `createdAt` `created_at` "
                    + "TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6)";
            if (executeIgnoreError(sql)) {
                log.info("业务库 {}：createdAt → created_at", table);
            }
        }
    }

    private void renameColumnIfExists(String table, String from, String to, String columnType) {
        if (!columnExists(table, from)) {
            return;
        }
        if (columnExists(table, to)) {
            if (executeIgnoreError("ALTER TABLE `" + table + "` DROP COLUMN `" + from + "`")) {
                log.info("业务库 {}：删除重复列 {}", table, from);
            }
            return;
        }
        String sql = "ALTER TABLE `" + table + "` CHANGE COLUMN `" + from + "` `" + to + "` " + columnType;
        if (executeIgnoreError(sql)) {
            log.info("业务库 {}：{} → {}", table, from, to);
        }
    }

    private boolean tableExists(String table) {
        Integer count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM information_schema.tables "
                        + "WHERE table_schema = DATABASE() AND table_name = ?",
                Integer.class,
                table);
        return count != null && count > 0;
    }

    private boolean columnExists(String table, String column) {
        Integer count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM information_schema.columns "
                        + "WHERE table_schema = DATABASE() AND table_name = ? AND column_name = ?",
                Integer.class,
                table,
                column);
        return count != null && count > 0;
    }

    private boolean executeIgnoreError(String sql) {
        try {
            jdbcTemplate.execute(sql);
            return true;
        } catch (Exception ex) {
            log.warn("业务库列迁移跳过（{}）: {}", sql, ex.getMessage());
            return false;
        }
    }
}
