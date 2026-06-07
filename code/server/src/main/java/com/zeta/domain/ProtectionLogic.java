package com.zeta.domain;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import javax.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "protection_logics")
@Getter
@Setter
@NoArgsConstructor
public class ProtectionLogic {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(
            name = "device_id",
            foreignKey = @ForeignKey(ConstraintMode.NO_CONSTRAINT))
    private Device device;

    @Column(nullable = false, unique = true, length = 64)
    private String code;

    @Column(nullable = false, length = 128)
    private String title;

    @Column(length = 512)
    private String description;

    @Column(nullable = false, length = 32)
    private String category;

    @Column(nullable = false)
    private Integer sortOrder = 0;

    /** 完整逻辑配置 JSON */
    @Lob
    @Column(nullable = false)
    private String configJson;

    @Column(nullable = false)
    private Boolean enabled = true;

    @Column(nullable = false)
    private Instant createdAt = Instant.now();
}
