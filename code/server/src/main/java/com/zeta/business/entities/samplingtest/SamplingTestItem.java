package com.zeta.business.entities.samplingtest;

import java.time.Instant;
import javax.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "sampling_test_items")
@Getter
@Setter
@NoArgsConstructor
public class SamplingTestItem {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(name = "screen_cabinet_id", nullable = false)
  private Long screenCabinetId;

  @Column(nullable = false, length = 128)
  private String title;

  @Enumerated(EnumType.STRING)
  @Column(name = "media_type", nullable = false, length = 32)
  private SamplingTestMediaType mediaType;

  @Column(name = "image_url", length = 512)
  private String imageUrl;

  @Lob
  @Column(name = "image_data")
  private byte[] imageData;

  @Column(name = "image_content_type", length = 100)
  private String imageContentType;

  @Column(name = "video_path", length = 512)
  private String videoPath;

  @Lob
  @Column(nullable = false)
  private String content;

  @Column(name = "sort_order", nullable = false)
  private Integer sortOrder = 0;

  @Column(nullable = false)
  private Boolean enabled = true;

  @Column(name = "created_at", nullable = false, updatable = false,
      columnDefinition = "TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6)")
  private Instant createdAt = Instant.now();
}
