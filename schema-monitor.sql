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
    id                  BIGINT UNSIGNED  NOT NULL AUTO_INCREMENT,
    cognition_device_id BIGINT UNSIGNED  NOT NULL COMMENT '认知设备 id',
    title               VARCHAR(128)     NOT NULL,
    image_url           VARCHAR(512)     NULL COMMENT '认知图片路径',
    image_data          LONGBLOB         NULL COMMENT '图片二进制数据',
    image_content_type  VARCHAR(100)     NULL COMMENT '图片 MIME 类型',
    media_type          VARCHAR(32)      NOT NULL DEFAULT 'IMAGE' COMMENT 'IMAGE / VIDEO / TERMINAL_OPERATION / IED_BASELINE_SETTING',
    video_path          VARCHAR(512)     NULL COMMENT 'JAR 同级视频相对路径',
    left_percent        DOUBLE           NULL,
    top_percent         DOUBLE           NULL,
    width_percent       DOUBLE           NULL,
    height_percent      DOUBLE           NULL,
    content             TEXT             NOT NULL,
    sort_order          INT              NOT NULL DEFAULT 0,
    enabled             TINYINT(1)       NOT NULL DEFAULT 1,
    created_at          TIMESTAMP(6)     NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    PRIMARY KEY (id),
    INDEX idx_ddi_cognition_device (cognition_device_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='认知设备展示条目';

CREATE TABLE IF NOT EXISTS terminal_operation_items (
    id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    device_display_item_id BIGINT UNSIGNED NOT NULL,
    terminal_strip_id BIGINT UNSIGNED NOT NULL,
    PRIMARY KEY (id), UNIQUE KEY uk_terminal_operation_item (device_display_item_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='端子操作认知条目';

CREATE TABLE IF NOT EXISTS terminal_operation_terminals (
    id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    terminal_operation_id BIGINT UNSIGNED NOT NULL,
    terminal_id BIGINT UNSIGNED NOT NULL,
    terminal_meaning VARCHAR(128) NULL COMMENT '历史端子表示含义，仅用于迁移追溯',
    expected_output_code VARCHAR(8) NULL COMMENT '预期试验仪语义输出代码',
    sort_order INT NOT NULL,
    PRIMARY KEY (id), INDEX idx_terminal_operation_terminal (terminal_operation_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='端子操作目标端子';

CREATE TABLE IF NOT EXISTS sampling_test_items (
    id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    screen_cabinet_id BIGINT UNSIGNED NOT NULL,
    title VARCHAR(128) NOT NULL,
    media_type VARCHAR(32) NOT NULL COMMENT 'IMAGE / VIDEO / SAMPLING_CONFIGURATION',
    image_url VARCHAR(512) NULL,
    image_data LONGBLOB NULL,
    image_content_type VARCHAR(100) NULL,
    video_path VARCHAR(512) NULL,
    content LONGTEXT NOT NULL,
    sort_order INT NOT NULL DEFAULT 0,
    enabled TINYINT(1) NOT NULL DEFAULT 1,
    created_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    PRIMARY KEY (id),
    INDEX idx_sampling_test_cabinet (screen_cabinet_id, sort_order, id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='采样测试认知条目';

CREATE TABLE IF NOT EXISTS sampling_test_channels (
    id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    sampling_test_item_id BIGINT UNSIGNED NOT NULL,
    output_code VARCHAR(2) NOT NULL,
    terminal_id BIGINT UNSIGNED NOT NULL,
    baseline_magnitude DECIMAL(18,6) NULL,
    baseline_angle DECIMAL(12,6) NULL,
    sort_order INT NOT NULL,
    PRIMARY KEY (id),
    UNIQUE KEY uk_sampling_channel_code (sampling_test_item_id, output_code),
    UNIQUE KEY uk_sampling_channel_terminal (sampling_test_item_id, terminal_id),
    INDEX idx_sampling_channel_item (sampling_test_item_id, sort_order, id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='采样测试八路通道配置';

-- ============================================================================
-- 软压板基准清单（引用屏柜库装置或逻辑框图 ID）
-- ============================================================================
CREATE TABLE IF NOT EXISTS soft_pressboard_list_item (
    id                BIGINT            NOT NULL AUTO_INCREMENT,
    scope_type        VARCHAR(32)       NOT NULL COMMENT 'IED_DEVICE 或 LOGIC_DIAGRAM',
    scope_id          BIGINT            NOT NULL COMMENT '屏柜库中的装置或逻辑框图 ID',
    pressboard_ref    VARCHAR(512)      NOT NULL,
    pressboard_name   VARCHAR(256)      NOT NULL,
    baseline_value    TINYINT(1)        NOT NULL COMMENT '0=退出，1=投入',
    compare_enabled   TINYINT(1)        NOT NULL DEFAULT 1,
    sort_order        INT               NOT NULL DEFAULT 0,
    created_at        TIMESTAMP         NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at        TIMESTAMP         NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    UNIQUE INDEX uk_soft_pressboard_list_scope_ref (scope_type, scope_id, pressboard_ref),
    INDEX idx_soft_pressboard_list_scope (scope_type, scope_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='逻辑学习软压板基准清单项';

-- ============================================================================
-- 试验仪接线要求（电压/电流各一套，ABCN 四端子一组）
-- ============================================================================
CREATE TABLE IF NOT EXISTS wiring_requirement_config (
    id                BIGINT            NOT NULL AUTO_INCREMENT,
    logic_diagram_id  BIGINT            NOT NULL COMMENT '屏柜库 logic_diagram.id',
    category          VARCHAR(16)       NOT NULL COMMENT 'VOLTAGE 或 CURRENT',
    required          TINYINT(1)        NOT NULL DEFAULT 0 COMMENT '是否需要接入/校验',
    phase_mode        VARCHAR(16)       NOT NULL DEFAULT 'THREE_PHASE' COMMENT 'THREE_PHASE 或 SINGLE_PHASE',
    created_at        TIMESTAMP         NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at        TIMESTAMP         NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    UNIQUE INDEX uk_wiring_req_config_logic_category (logic_diagram_id, category)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='逻辑框图试验仪接线要求配置（电压/电流各一套）';

CREATE TABLE IF NOT EXISTS wiring_requirement_group (
    id              BIGINT            NOT NULL AUTO_INCREMENT,
    config_id       BIGINT            NOT NULL COMMENT '所属 wiring_requirement_config.id',
    group_no        INT               NOT NULL DEFAULT 0 COMMENT '组序号',
    terminal_a_id   BIGINT            NULL COMMENT 'A相端子 terminal.id',
    terminal_b_id   BIGINT            NULL COMMENT 'B相端子 terminal.id',
    terminal_c_id   BIGINT            NULL COMMENT 'C相端子 terminal.id',
    terminal_n_id   BIGINT            NULL COMMENT 'N相端子 terminal.id',
    created_at      TIMESTAMP         NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at      TIMESTAMP         NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    INDEX idx_wiring_req_group_config (config_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='试验仪接线要求端子分组（ABCN 一组）';

-- ============================================================================
-- 屏柜认知图上的抽象设备（IED 外观 / IED 操作 / 其他设备 / 端子组 / 压板组）
-- ============================================================================
CREATE TABLE IF NOT EXISTS cognition_devices (
    id                      BIGINT UNSIGNED  NOT NULL AUTO_INCREMENT,
    cabinet_display_item_id BIGINT UNSIGNED  NOT NULL COMMENT '所属屏柜认知条目',
    device_type             VARCHAR(32)      NOT NULL COMMENT 'IED（外观）/ IED_OPERATION / OTHER_DEVICE / TERMINAL_GROUP / PLATE_GROUP',
    screen_device_id        BIGINT UNSIGNED  NULL COMMENT 'IED 外观或 IED 操作类型时引用 ied_device.id',
    title                   VARCHAR(128)     NOT NULL,
    left_percent            DOUBLE           NOT NULL,
    top_percent             DOUBLE           NOT NULL,
    width_percent           DOUBLE           NOT NULL,
    height_percent          DOUBLE           NOT NULL,
    sort_order              INT              NOT NULL DEFAULT 0,
    enabled                 TINYINT(1)       NOT NULL DEFAULT 1,
    created_at              TIMESTAMP(6)     NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    PRIMARY KEY (id),
    INDEX idx_cd_cabinet_item (cabinet_display_item_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='屏柜认知图上的抽象设备';

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

-- ============================================================================
-- 逻辑节点认知条目
-- ============================================================================
CREATE TABLE IF NOT EXISTS logic_node_cognition_items (
    id                  BIGINT UNSIGNED  NOT NULL AUTO_INCREMENT,
    logic_diagram_id    BIGINT UNSIGNED  NOT NULL,
    node_id             VARCHAR(128)     NOT NULL,
    node_type           VARCHAR(32)      NOT NULL,
    node_name           VARCHAR(256)     NOT NULL,
    title               VARCHAR(128)     NOT NULL,
    image_url           VARCHAR(512)     NULL,
    image_data          LONGBLOB         NULL,
    image_content_type  VARCHAR(100)     NULL,
    media_type          VARCHAR(16)      NOT NULL DEFAULT 'IMAGE' COMMENT 'IMAGE / VIDEO / TEXT',
    video_path          VARCHAR(512)     NULL COMMENT 'JAR 同级视频相对路径',
    left_percent        DOUBLE           NULL,
    top_percent         DOUBLE           NULL,
    width_percent       DOUBLE           NULL,
    height_percent      DOUBLE           NULL,
    content             LONGTEXT         NOT NULL,
    sort_order          INT              NOT NULL DEFAULT 0,
    enabled             TINYINT(1)       NOT NULL DEFAULT 1,
    created_at          TIMESTAMP(6)     NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    PRIMARY KEY (id),
    INDEX idx_logic_node_cognition_node (logic_diagram_id, node_id),
    INDEX idx_logic_node_cognition_order (logic_diagram_id, node_id, sort_order, id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='逻辑节点认知条目';
