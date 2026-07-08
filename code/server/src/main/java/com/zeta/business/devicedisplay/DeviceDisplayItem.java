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

    /** 图片访问路径（兼容旧数据） */
    @Column(name = "image_url", length = 512)
    private String imageUrl;

    /** 图片二进制数据 */
    @Lob
    @Column(name = "image_data")
    private byte[] imageData;

    /** 图片 MIME 类型 */
    @Column(name = "image_content_type", length = 100)
    private String imageContentType;

    /** 条目图片上的高亮区域，可为空表示不显示高亮 */
    @Column(name = "left_percent")
    private Double leftPercent;

    @Column(name = "top_percent")
    private Double topPercent;

    @Column(name = "width_percent")
    private Double widthPercent;

    @Column(name = "height_percent")
    private Double heightPercent;

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
