package com.zeta.business.binding;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import javax.persistence.*;
import java.time.Instant;

/**
 * 平板设备与屏柜的绑定关系。
 * 存储在 ct-screen-monitor（业务库），通过 screen_cabinet_id 跨库引用 ct-screen.cabinet。
 */
@Entity
@Table(name = "cabinet_binding", indexes = {
        @Index(name = "idx_cb_cabinet", columnList = "screen_cabinet_id", unique = true),
        @Index(name = "idx_cb_bind_id", columnList = "bind_id", unique = true)
})
@Getter
@Setter
@NoArgsConstructor
public class CabinetBinding {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** 跨库引用 ct-screen.cabinet.id，每个屏柜最多一条绑定 */
    @Column(name = "screen_cabinet_id", nullable = false, unique = true)
    private Long screenCabinetId;

    /** 平板设备唯一标识（UUID），全局唯一 */
    @Column(name = "bind_id", nullable = false, unique = true, length = 64)
    private String bindId;

    /** 绑定标签（如"1号平板"），全局唯一 */
    @Column(name = "bind_label", nullable = false, unique = true, length = 128)
    private String bindLabel;

    @Column(name = "created_at", nullable = false, updatable = false,
            columnDefinition = "TIMESTAMP(6) DEFAULT CURRENT_TIMESTAMP(6)")
    private Instant createdAt;
}
