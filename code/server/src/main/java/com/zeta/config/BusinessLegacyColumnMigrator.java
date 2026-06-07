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
