package com.zeta.business.entities.drawinglearning;

import com.zeta.business.entities.drawinglearning.dto.*;
import java.time.Instant;
import javax.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "drawing_cognition_items")
@Getter
@Setter
@NoArgsConstructor
public class DrawingCognitionItem {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(name = "drawing_page_id", nullable = false)
  private Long drawingPageId;

  @Column(nullable = false, length = 128)
  private String title;

  @Lob
  @Column(nullable = false)
  private String content;

  @Column(name = "left_percent")
  private Double leftPercent;

  @Column(name = "top_percent")
  private Double topPercent;

  @Column(name = "width_percent")
  private Double widthPercent;

  @Column(name = "height_percent")
  private Double heightPercent;

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
