-- ============================================================================
-- ct-screen 数据库结构定义
-- 继电保护试验过程检测与评估系统
-- MySQL 8.0+ / InnoDB / utf8mb4
--
-- 本文件由底层库（硬件侧）维护，业务系统只读。
-- 最后对齐时间：2026-07-01
-- ============================================================================

CREATE DATABASE IF NOT EXISTS ct_screen
  DEFAULT CHARACTER SET utf8mb4
  DEFAULT COLLATE utf8mb4_unicode_ci;

USE ct_screen;

-- ============================================================================
-- 1. 屏柜信息表
-- ============================================================================
CREATE TABLE cabinet (
    id              BIGINT UNSIGNED  NOT NULL AUTO_INCREMENT  COMMENT '屏柜唯一标识',
    name            VARCHAR(128)     NOT NULL                 COMMENT '屏柜名称',
    location        VARCHAR(256)     DEFAULT NULL             COMMENT '安装位置描述',
    description     TEXT             DEFAULT NULL             COMMENT '屏柜备注说明',
    created_at      TIMESTAMP        NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at      TIMESTAMP        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,

    PRIMARY KEY (id),
    UNIQUE INDEX uk_cabinet_name (name),
    INDEX idx_cabinet_location (location)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='屏柜信息表';


-- ============================================================================
-- 2. IED 保护装置信息表
-- ============================================================================
CREATE TABLE ied_device (
    id              BIGINT UNSIGNED  NOT NULL AUTO_INCREMENT  COMMENT 'IED 装置唯一标识',
    cabinet_id      BIGINT UNSIGNED  NOT NULL                 COMMENT '所属屏柜 ID',
    ied_name        VARCHAR(128)     NOT NULL                 COMMENT 'IED 名称（SCD 模型中的 IED name）',
    device_type     VARCHAR(64)      DEFAULT NULL             COMMENT '装置型号',
    manufacturer    VARCHAR(128)     DEFAULT NULL             COMMENT '制造厂商',
    ied_desc        VARCHAR(256)     DEFAULT NULL             COMMENT 'IED 描述（SCD 模型中的 desc 字段）',
    config_version  VARCHAR(32)      DEFAULT NULL             COMMENT 'SCD 配置版本号',
    created_at      TIMESTAMP        NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at      TIMESTAMP        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,

    PRIMARY KEY (id),
    UNIQUE INDEX uk_ied_cabinet_name (cabinet_id, ied_name),
    INDEX idx_ied_cabinet (cabinet_id),
    INDEX idx_ied_name (ied_name)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='IED 保护装置信息表';


-- ============================================================================
-- 3. 连线检测装置信息表
-- ============================================================================
CREATE TABLE wiring_detection_device (
    id                 BIGINT UNSIGNED  NOT NULL AUTO_INCREMENT  COMMENT '连线检测装置唯一标识',
    cabinet_id         BIGINT UNSIGNED  NOT NULL                 COMMENT '所属屏柜 ID',
    device_name        VARCHAR(128)     NOT NULL                 COMMENT '装置名称',
    device_type        VARCHAR(64)      DEFAULT NULL             COMMENT '装置型号',
    modbus_server_ip   VARCHAR(45)      NOT NULL                 COMMENT 'Modbus 服务端 IP 地址',
    modbus_server_port INT              DEFAULT 502              COMMENT 'Modbus 服务端端口号',
    modbus_slave_id    INT              DEFAULT 1                COMMENT 'Modbus 从站地址',
    modbus_timeout_ms  INT              DEFAULT 5000             COMMENT 'Modbus 通信超时时间（毫秒）',
    description        TEXT             DEFAULT NULL             COMMENT '装置备注说明',
    created_at         TIMESTAMP        NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at         TIMESTAMP        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,

    PRIMARY KEY (id),
    INDEX idx_wdd_cabinet (cabinet_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='连线检测装置信息表';


-- ============================================================================
-- 4. 连线检测信号表
-- ============================================================================
CREATE TABLE wiring_detection_signal (
    id              BIGINT UNSIGNED  NOT NULL AUTO_INCREMENT,
    device_id       BIGINT UNSIGNED  NOT NULL                 COMMENT '所属连线检测装置 ID',
    signal_name     VARCHAR(128)     NOT NULL                 COMMENT '信号名称',
    signal_index    INT              NOT NULL DEFAULT 0       COMMENT '信号序号',
    description     TEXT             DEFAULT NULL,
    created_at      TIMESTAMP        NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at      TIMESTAMP        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,

    PRIMARY KEY (id),
    INDEX idx_wds_device (device_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='连线检测信号表';


-- ============================================================================
-- 5. 端子排表
-- ============================================================================
CREATE TABLE terminal_strip (
    id              BIGINT UNSIGNED  NOT NULL AUTO_INCREMENT,
    cabinet_id      BIGINT UNSIGNED  NOT NULL                 COMMENT '所属屏柜 ID',
    name            VARCHAR(128)     NOT NULL                 COMMENT '端子排名称',
    label_prefix    VARCHAR(64)      NOT NULL                 COMMENT '编号前缀',
    function_desc   VARCHAR(256)     DEFAULT NULL             COMMENT '功能描述',
    sort_order      INT              NOT NULL DEFAULT 0,
    description     TEXT             DEFAULT NULL,
    created_at      TIMESTAMP        NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at      TIMESTAMP        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,

    PRIMARY KEY (id),
    INDEX idx_ts_cabinet (cabinet_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='端子排表';


-- ============================================================================
-- 6. 端子表
-- ============================================================================
CREATE TABLE terminal (
    id                      BIGINT UNSIGNED  NOT NULL AUTO_INCREMENT,
    cabinet_id              BIGINT UNSIGNED  NOT NULL                 COMMENT '所属屏柜 ID',
    terminal_strip_id       BIGINT UNSIGNED  DEFAULT NULL             COMMENT '所属端子排 ID',
    terminal_label          VARCHAR(64)      NOT NULL                 COMMENT '端子编号',
    signal_type             ENUM('DIGITAL','ANALOG','END','DO') NOT NULL COMMENT '信号类型',
    ied_device_id           BIGINT UNSIGNED  DEFAULT NULL             COMMENT '关联 IED 装置 ID',
    ied_signal_ref          VARCHAR(512)     DEFAULT NULL             COMMENT 'IED MMS 信号引用路径',
    common_end_terminal_id  BIGINT UNSIGNED  DEFAULT NULL             COMMENT '公共端端子 ID',
    common_end_group_id     BIGINT UNSIGNED  DEFAULT NULL             COMMENT '公共端组 ID',
    description             TEXT             DEFAULT NULL,
    created_at              TIMESTAMP        NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at              TIMESTAMP        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,

    PRIMARY KEY (id),
    INDEX idx_terminal_cabinet (cabinet_id),
    INDEX idx_terminal_strip (terminal_strip_id),
    INDEX idx_terminal_ied (ied_device_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='端子表';


-- ============================================================================
-- 7. 公共端组表
-- ============================================================================
CREATE TABLE common_end_group (
    id                  BIGINT UNSIGNED  NOT NULL AUTO_INCREMENT,
    cabinet_id          BIGINT UNSIGNED  NOT NULL             COMMENT '所属屏柜 ID',
    terminal_strip_id   BIGINT UNSIGNED  NOT NULL             COMMENT '所属端子排 ID',
    name                VARCHAR(128)     NOT NULL             COMMENT '公共端组名称',
    description         TEXT             DEFAULT NULL,
    created_at          TIMESTAMP        NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at          TIMESTAMP        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,

    PRIMARY KEY (id),
    INDEX idx_ceg_cabinet (cabinet_id),
    INDEX idx_ceg_strip (terminal_strip_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='公共端组表';


-- ============================================================================
-- 8. 硬压板表
-- ============================================================================
CREATE TABLE hard_pressboard (
    id                          BIGINT UNSIGNED  NOT NULL AUTO_INCREMENT,
    cabinet_id                  BIGINT UNSIGNED  NOT NULL             COMMENT '所属屏柜 ID',
    name                        VARCHAR(128)     NOT NULL             COMMENT '压板名称',
    pressboard_type             ENUM('FUNCTION','EXPORT','SPARE') NOT NULL DEFAULT 'FUNCTION' COMMENT '压板类型',
    row_no                      INT              NOT NULL DEFAULT 1   COMMENT '行号',
    col_no                      INT              NOT NULL DEFAULT 1   COMMENT '列号',
    ied_device_id               BIGINT UNSIGNED  DEFAULT NULL         COMMENT '关联 IED 装置 ID',
    ied_signal_ref              VARCHAR(512)     DEFAULT NULL         COMMENT 'IED 信号引用',
    wiring_detection_device_id  BIGINT UNSIGNED  DEFAULT NULL         COMMENT '关联连线检测装置 ID',
    input_channel_no            INT              DEFAULT NULL         COMMENT '输入通道号',
    output_channel_no           INT              DEFAULT NULL         COMMENT '输出通道号',
    description                 TEXT             DEFAULT NULL,
    created_at                  TIMESTAMP        NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at                  TIMESTAMP        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,

    PRIMARY KEY (id),
    INDEX idx_hp_cabinet (cabinet_id),
    INDEX idx_hp_ied (ied_device_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='硬压板表';


-- ============================================================================
-- 9. IED 基准定值项表
-- ============================================================================
CREATE TABLE ied_baseline_setting_item (
    id                   BIGINT UNSIGNED  NOT NULL AUTO_INCREMENT,
    ied_device_id        BIGINT UNSIGNED  NOT NULL             COMMENT '所属 IED 装置 ID',
    setting_ref          VARCHAR(512)     NOT NULL             COMMENT '定值引用路径（SCD）',
    setting_fc           VARCHAR(16)      DEFAULT 'SG'         COMMENT '功能约束',
    setting_description  VARCHAR(256)     DEFAULT NULL         COMMENT '定值描述',
    baseline_value       VARCHAR(64)      NOT NULL             COMMENT '基准值',
    sort_order           INT              NOT NULL DEFAULT 0,
    created_at           TIMESTAMP        NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at           TIMESTAMP        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,

    PRIMARY KEY (id),
    INDEX idx_ibs_ied (ied_device_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='IED 基准定值项表';


-- ============================================================================
-- 10. 逻辑框图基准定值项表
-- ============================================================================
CREATE TABLE logic_diagram_baseline_setting_item (
    id                   BIGINT UNSIGNED  NOT NULL AUTO_INCREMENT,
    logic_diagram_id     BIGINT UNSIGNED  NOT NULL             COMMENT '所属逻辑框图 ID',
    setting_ref          VARCHAR(512)     NOT NULL             COMMENT '定值引用路径（SCD）',
    setting_fc           VARCHAR(16)      DEFAULT 'SG'         COMMENT '功能约束',
    setting_description  VARCHAR(256)     DEFAULT NULL         COMMENT '定值描述',
    baseline_value       VARCHAR(64)      NOT NULL             COMMENT '基准值',
    sort_order           INT              NOT NULL DEFAULT 0,
    created_at           TIMESTAMP        NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at           TIMESTAMP        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,

    PRIMARY KEY (id),
    INDEX idx_ldbs_logic (logic_diagram_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='逻辑框图基准定值项表';


-- ============================================================================
-- 11. 逻辑框图配置表
-- ============================================================================
CREATE TABLE logic_diagram (
    id              BIGINT UNSIGNED  NOT NULL AUTO_INCREMENT  COMMENT '逻辑图唯一标识',
    ied_device_id   BIGINT UNSIGNED  NOT NULL                 COMMENT '所属 IED 装置 ID',
    logic_id        VARCHAR(128)     NOT NULL                 COMMENT '逻辑图业务标识',
    logic_name      VARCHAR(256)     NOT NULL                 COMMENT '逻辑图名称',
    protect_type    VARCHAR(64)      DEFAULT NULL             COMMENT '保护类型',
    version         VARCHAR(16)      NOT NULL DEFAULT '1.0'   COMMENT '配置格式版本号',
    description     TEXT             DEFAULT NULL             COMMENT '逻辑图描述说明',
    config_json     LONGTEXT         NOT NULL                 COMMENT '完整的 ProtectionConfig JSON',
    created_at      TIMESTAMP        NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at      TIMESTAMP        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,

    PRIMARY KEY (id),
    UNIQUE INDEX uk_ld_ied_logic (ied_device_id, logic_id),
    INDEX idx_ld_ied (ied_device_id),
    INDEX idx_ld_logic_id (logic_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='保护逻辑框图配置表';


-- ============================================================================
-- 12. 端子连线判据表
-- ============================================================================
CREATE TABLE terminal_wiring_criterion (
    id                          BIGINT UNSIGNED  NOT NULL AUTO_INCREMENT,
    terminal_id                 BIGINT UNSIGNED  NOT NULL             COMMENT '关联端子 ID',
    wiring_detection_device_id  BIGINT UNSIGNED  NOT NULL             COMMENT '关联连线检测装置 ID',
    criterion_type              ENUM('NORMAL','ABNORMAL') NOT NULL    COMMENT '判据类型',
    name                        VARCHAR(128)     NOT NULL             COMMENT '判据名称',
    abnormal_message            VARCHAR(256)     DEFAULT NULL         COMMENT '异常提示信息',
    sort_order                  INT              NOT NULL DEFAULT 0,
    created_at                  TIMESTAMP        NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at                  TIMESTAMP        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,

    PRIMARY KEY (id),
    INDEX idx_twc_terminal (terminal_id),
    INDEX idx_twc_device (wiring_detection_device_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='端子连线判据表';


-- ============================================================================
-- 13. 端子连线判据通道对表
-- ============================================================================
CREATE TABLE terminal_wiring_criterion_pair (
    id                  BIGINT UNSIGNED  NOT NULL AUTO_INCREMENT,
    criterion_id        BIGINT UNSIGNED  NOT NULL             COMMENT '所属判据 ID',
    input_channel_no    INT              NOT NULL             COMMENT '输入通道号',
    output_channel_no   INT              NOT NULL             COMMENT '输出通道号',
    match_mode          ENUM('SATISFIED','NOT_SATISFIED') NOT NULL DEFAULT 'SATISFIED' COMMENT '匹配模式',
    created_at          TIMESTAMP        NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at          TIMESTAMP        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,

    PRIMARY KEY (id),
    INDEX idx_twcp_criterion (criterion_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='端子连线判据通道对表';


-- ============================================================================
-- 14. 实验监控任务表（乙方系统写入，本系统读取）
-- ============================================================================
CREATE TABLE monitor_task (
    id                    BIGINT UNSIGNED  NOT NULL AUTO_INCREMENT,
    uuid                  CHAR(36)         NOT NULL                 COMMENT '任务唯一 UUID',
    ied_device_id         BIGINT UNSIGNED  NOT NULL                 COMMENT '关联 IED 装置 ID',
    logic_diagram_id      BIGINT UNSIGNED  NOT NULL                 COMMENT '关联逻辑框图 ID',
    state                 ENUM('PENDING','INITIALIZING','SUMMONING_SETTINGS','WAITING_RCD_MADE',
                               'SUMMONING_FILES','PARSING_FILES','GENERATING_SNAPSHOT',
                               'COMPLETED','FAILED','TIMEOUT','CANCELLED')
                                          NOT NULL DEFAULT 'PENDING' COMMENT '任务状态',
    error_code            VARCHAR(64)      DEFAULT NULL             COMMENT '错误码',
    error_message         TEXT             DEFAULT NULL             COMMENT '错误信息',
    total_transitions     INT              DEFAULT 0                COMMENT '总变位次数',
    snapshot_json         LONGTEXT         DEFAULT NULL             COMMENT '断面快照 JSON',
    comtrade_cfg          MEDIUMBLOB       DEFAULT NULL             COMMENT 'COMTRADE 配置文件',
    comtrade_dat          LONGBLOB         DEFAULT NULL             COMMENT 'COMTRADE 数据文件',
    comtrade_mid          MEDIUMBLOB       DEFAULT NULL             COMMENT 'COMTRADE 中间文件',
    comtrade_des          MEDIUMBLOB       DEFAULT NULL             COMMENT 'COMTRADE 描述文件',
    comtrade_hdr          MEDIUMBLOB       DEFAULT NULL             COMMENT 'COMTRADE 头文件',
    started_at            TIMESTAMP        NULL     DEFAULT NULL    COMMENT '任务开始时间',
    completed_at          TIMESTAMP        NULL     DEFAULT NULL    COMMENT '任务完成时间',
    created_at            TIMESTAMP        NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at            TIMESTAMP        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,

    PRIMARY KEY (id),
    UNIQUE INDEX uk_mt_uuid (uuid),
    INDEX idx_mt_ied (ied_device_id),
    INDEX idx_mt_logic (logic_diagram_id),
    INDEX idx_mt_state (state),
    INDEX idx_mt_created (created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='实验监控任务表';
