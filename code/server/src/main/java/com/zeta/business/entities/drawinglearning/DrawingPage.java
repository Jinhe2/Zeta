package com.zeta.business.entities.drawinglearning;

import com.zeta.business.entities.drawinglearning.dto.*;
import java.time.Instant;
import javax.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "drawing_pages")
@Getter
@Setter
@NoArgsConstructor
public class DrawingPage {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(name = "drawing_group_id", nullable = false)
  private Long drawingGroupId;

  @Column(nullable = false, length = 128)
  private String title;

  @Column(name = "image_url", length = 512)
  private String imageUrl;

  @Lob
  @Column(name = "image_data")
  private byte[] imageData;

  @Column(name = "image_content_type", length = 100)
  private String imageContentType;

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
