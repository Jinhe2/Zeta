package com.zeta.domain;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import javax.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "device_cognition_items")
@Getter
@Setter
@NoArgsConstructor
public class DeviceCognitionItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(
            name = "device_id",
            foreignKey = @ForeignKey(ConstraintMode.NO_CONSTRAINT))
    private Device device;

    @Column(nullable = false, length = 128)
    private String title;

    @Lob
    @Column(nullable = false)
    private String content;

    @Column(nullable = false)
    private Integer sortOrder = 0;

    @Column(nullable = false)
    private Boolean enabled = true;

    @Column(nullable = false)
    private Instant createdAt = Instant.now();
}
