package com.zeta.business.cabinetdisplay;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import javax.persistence.*;
import java.time.Instant;

/**
 * 屏柜认知条目 — 业务库（ct-screen-monitor）。
 * 每条包含一张图片与一段文字描述，通过 screenCabinetId 引用屏柜库 cabinet 表主键。
 */
@Entity
@Table(name = "cabinet_display_items")
@Getter
@Setter
@NoArgsConstructor
public class CabinetDisplayItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** 屏柜库 ct-screen.cabinet.id */
    @Column(name = "screen_cabinet_id", nullable = false)
    private Long screenCabinetId;

    /** 条目名称，如正视图、侧视图等 */
    @Column(nullable = false, length = 128)
    private String title;

    /** 图片访问路径 */
    @Column(name = "image_url", nullable = false, length = 512)
    private String imageUrl;

    /** 文字描述 */
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
