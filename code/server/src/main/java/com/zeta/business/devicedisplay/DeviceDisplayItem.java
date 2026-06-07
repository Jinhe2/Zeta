package com.zeta.business.devicedisplay;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import javax.persistence.*;
import java.time.Instant;

/**
 * 设备展示条目 — 业务库（ct-screen-monitor）展示/介绍数据。
 * 通过 screenDeviceId 引用屏柜库 ied_device 表主键，跨库无外键。
 */
@Entity
@Table(name = "device_display_items")
@Getter
@Setter
@NoArgsConstructor
public class DeviceDisplayItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** 屏柜库 ct-screen.ied_device.id */
    @Column(name = "screen_device_id", nullable = false)
    private Long screenDeviceId;

    @Column(nullable = false, length = 128)
    private String title;

    @Lob
    @Column(nullable = false)
    private String content;

    @Column(name = "sort_order", nullable = false)
    private Integer sortOrder = 0;

    @Column(nullable = false)
    private Boolean enabled = true;

    @Column(
            name = "created_at",
            nullable = false,
            updatable = false,
            columnDefinition = "TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6)")
    private Instant createdAt = Instant.now();
}
