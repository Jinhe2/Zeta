package com.zeta.screen.wiring;

import com.zeta.screen.cabinet.Cabinet;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import javax.persistence.*;
import java.time.Instant;

/** 连线检测装置，对应 ct-screen.wiring_detection_device */
@Entity
@Table(name = "wiring_detection_device")
@Getter
@Setter
@NoArgsConstructor
public class WiringDetectionDevice {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "cabinet_id", nullable = false)
    private Cabinet cabinet;

    @Column(name = "device_name", nullable = false, length = 128)
    private String deviceName;

    @Column(name = "device_type", length = 64)
    private String deviceType;

    @Column(name = "modbus_server_ip", nullable = false, length = 45)
    private String modbusServerIp;

    @Column(name = "modbus_server_port")
    private Integer modbusServerPort;

    @Column(name = "modbus_slave_id")
    private Integer modbusSlaveId;

    @Column(name = "modbus_timeout_ms")
    private Integer modbusTimeoutMs;

    @Lob
    private String description;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;
}
