package com.zeta.business.cabinetdisplay;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import javax.persistence.*;
import java.time.Instant;

/**
 * 临时图片存储 — 用于上传后、创建记录前的中间存储。
 * 记录创建成功后应删除对应的临时记录。
 */
@Entity
@Table(name = "temporary_images")
@Getter
@Setter
@NoArgsConstructor
public class TemporaryImage {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Lob
    @Column(name = "image_data", nullable = false)
    private byte[] imageData;

    @Column(name = "content_type", nullable = false, length = 100)
    private String contentType;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt = Instant.now();
}
