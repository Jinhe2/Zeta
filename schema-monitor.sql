-- ============================================================================
-- ct-screen-monitor 业务库结构定义
-- MySQL 8.0+ / InnoDB / utf8mb4
-- ============================================================================

CREATE DATABASE IF NOT EXISTS `ct-screen-monitor`
  DEFAULT CHARACTER SET utf8mb4
  DEFAULT COLLATE utf8mb4_unicode_ci;

USE `ct-screen-monitor`;

-- ============================================================================
-- 用户
-- ============================================================================
CREATE TABLE IF NOT EXISTS users (
    id              BIGINT UNSIGNED  NOT NULL AUTO_INCREMENT,
    username        VARCHAR(64)      NOT NULL,
    password        VARCHAR(128)     NOT NULL,
    display_name    VARCHAR(64)      NOT NULL,
    role            VARCHAR(16)      NOT NULL,
    created_at      TIMESTAMP(6)     NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    PRIMARY KEY (id),
    UNIQUE INDEX uk_users_username (username)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='系统用户';

-- ============================================================================
-- 设备展示条目（引用 ct-screen.ied_device.id）
-- ============================================================================
CREATE TABLE IF NOT EXISTS device_display_items (
    id                BIGINT UNSIGNED  NOT NULL AUTO_INCREMENT,
    screen_device_id  BIGINT UNSIGNED  NOT NULL,
    title             VARCHAR(128)     NOT NULL,
    content           TEXT             NOT NULL,
    sort_order        INT              NOT NULL DEFAULT 0,
    enabled           TINYINT(1)       NOT NULL DEFAULT 1,
    created_at        TIMESTAMP(6)     NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    PRIMARY KEY (id),
    INDEX idx_ddi_device (screen_device_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='设备展示条目';

-- ============================================================================
-- 屏柜认知条目（引用 ct-screen.cabinet.id）
-- ============================================================================
CREATE TABLE IF NOT EXISTS cabinet_display_items (
    id                  BIGINT UNSIGNED  NOT NULL AUTO_INCREMENT,
    screen_cabinet_id   BIGINT UNSIGNED  NOT NULL,
    title               VARCHAR(128)     NOT NULL COMMENT '条目名称，如正视图、侧视图',
    image_url           VARCHAR(512)     NOT NULL COMMENT '认知图片路径',
    content             TEXT             NOT NULL COMMENT '文字描述',
    sort_order          INT              NOT NULL DEFAULT 0,
    enabled             TINYINT(1)       NOT NULL DEFAULT 1,
    created_at          TIMESTAMP(6)     NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    PRIMARY KEY (id),
    INDEX idx_cdi_cabinet (screen_cabinet_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='屏柜认知条目';
