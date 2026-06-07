package com.zeta.business.cabinetdisplay;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import javax.persistence.*;
import java.time.Instant;

/**
 * 屏柜展示条目 — 业务库（ct-screen-monitor）展示/介绍数据。
 * 通过 screenCabinetId 引用屏柜库 cabinet 表主键，跨库无外键。
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
