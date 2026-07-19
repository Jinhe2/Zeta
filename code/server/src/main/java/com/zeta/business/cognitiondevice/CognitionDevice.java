package com.zeta.business.cognitiondevice;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import javax.persistence.*;
import java.time.Instant;

/**
 * 屏柜学习图上的抽象设备 — 隶属于某条 {@code cabinet_display_items}，
 * 在父图上有区域坐标；IED 外观和 IED 操作类型可关联屏柜库 IED。
 */
@Entity
@Table(name = "cognition_devices")
@Getter
@Setter
@NoArgsConstructor
public class CognitionDevice {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "cabinet_display_item_id", nullable = false)
    private Long cabinetDisplayItemId;

    @Enumerated(EnumType.STRING)
    @Column(name = "device_type", nullable = false, length = 32)
    private CognitionDeviceType deviceType;

    /** IED 外观或 IED 操作类型时引用 ct-screen.ied_device.id */
    @Column(name = "screen_device_id")
    private Long screenDeviceId;

    @Column(nullable = false, length = 128)
    private String title;

    @Column(name = "left_percent", nullable = false)
    private Double leftPercent;

    @Column(name = "top_percent", nullable = false)
    private Double topPercent;

    @Column(name = "width_percent", nullable = false)
    private Double widthPercent;

    @Column(name = "height_percent", nullable = false)
    private Double heightPercent;

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
