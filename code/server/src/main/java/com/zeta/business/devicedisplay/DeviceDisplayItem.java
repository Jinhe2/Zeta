package com.zeta.business.devicedisplay;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import javax.persistence.*;
import java.time.Instant;

/**
 * 认知设备下的展示条目（图片 + 文字），隶属于 {@code cognition_devices}。
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

    @Column(name = "cognition_device_id", nullable = false)
    private Long cognitionDeviceId;

    @Column(nullable = false, length = 128)
    private String title;

    @Column(name = "image_url", nullable = false, length = 512)
    private String imageUrl;

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
