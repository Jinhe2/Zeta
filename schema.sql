-- ============================================================================
-- ct-screen 数据库结构定义
-- 继电保护试验过程检测与评估系统
-- MySQL 8.0+ / InnoDB / utf8mb4
-- ============================================================================

CREATE DATABASE IF NOT EXISTS ct-screen
  DEFAULT CHARACTER SET utf8mb4
  DEFAULT COLLATE utf8mb4_unicode_ci;

USE ct-screen;

-- ============================================================================
-- 1. 屏柜信息表
-- ============================================================================
CREATE TABLE cabinet (
    id              BIGINT UNSIGNED  NOT NULL AUTO_INCREMENT  COMMENT '屏柜唯一标识',
    name            VARCHAR(128)     NOT NULL                 COMMENT '屏柜名称',
    location        VARCHAR(256)     DEFAULT NULL             COMMENT '安装位置描述',
    description     TEXT             DEFAULT NULL             COMMENT '屏柜备注说明',
    created_at      TIMESTAMP        NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at      TIMESTAMP        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '最后更新时间',

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
    created_at      TIMESTAMP        NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at      TIMESTAMP        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '最后更新时间',

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
    created_at         TIMESTAMP        NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at         TIMESTAMP        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '最后更新时间',

    PRIMARY KEY (id),
    INDEX idx_wdd_cabinet (cabinet_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='连线检测装置信息表';



-- ============================================================================
-- 5. 端子连线表（核心映射表）
-- ============================================================================
CREATE TABLE terminal_connection (
    id               BIGINT UNSIGNED  NOT NULL AUTO_INCREMENT  COMMENT '端子连线唯一标识',
    ied_device_id    BIGINT UNSIGNED  NOT NULL                 COMMENT '所属 IED 装置 ID',
    wiring_signal_id BIGINT UNSIGNED  NOT NULL                 COMMENT '关联的连线检测信号 ID（1:1）',
    signal_type      ENUM('DIGITAL','ANALOG') NOT NULL         COMMENT '信号类型：DIGITAL-开关量，ANALOG-模拟量',
    ied_signal_ref   VARCHAR(512)     NOT NULL                 COMMENT 'IED MMS 信号引用路径',
    terminal_label   VARCHAR(64)      DEFAULT NULL             COMMENT '端子编号标签',
    description      TEXT             DEFAULT NULL             COMMENT '连线说明',
    created_at       TIMESTAMP        NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at       TIMESTAMP        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '最后更新时间',

    PRIMARY KEY (id),
    UNIQUE INDEX uk_tc_wiring_signal (wiring_signal_id),
    INDEX idx_tc_ied (ied_device_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='端子连线表，连接 IED 信号与连线检测信号';


-- ============================================================================
-- 6. 逻辑框图配置表
-- ============================================================================
CREATE TABLE logic_diagram (
    id              BIGINT UNSIGNED  NOT NULL AUTO_INCREMENT  COMMENT '逻辑图唯一标识',
    ied_device_id   BIGINT UNSIGNED  NOT NULL                 COMMENT '所属 IED 装置 ID',
    logic_id        VARCHAR(128)     NOT NULL                 COMMENT '逻辑图业务标识',
    logic_name      VARCHAR(256)     NOT NULL                 COMMENT '逻辑图名称',
    protect_type    VARCHAR(64)      DEFAULT NULL             COMMENT '保护类型',
    version         VARCHAR(16)      NOT NULL DEFAULT '1.0'   COMMENT '配置格式版本号',
    description     TEXT             DEFAULT NULL             COMMENT '逻辑图描述说明',
    config_json     JSON             NOT NULL                 COMMENT '完整的 ProtectionConfig JSON',
    created_at      TIMESTAMP        NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at      TIMESTAMP        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '最后更新时间',

    PRIMARY KEY (id),
    UNIQUE INDEX uk_ld_ied_logic (ied_device_id, logic_id),
    INDEX idx_ld_ied (ied_device_id),
    INDEX idx_ld_logic_id (logic_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='保护逻辑框图配置表';

