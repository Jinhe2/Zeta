package com.zeta.business.entities.experimentguide;

import com.zeta.business.entities.settinglist.SettingListScopeType;
import java.time.Instant;
import javax.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** 实验引导条目，挂在基础逻辑（LOGIC_DIAGRAM）或组合逻辑（LOGIC_GROUP）作用域下。 */
@Entity
@Table(name = "experiment_guide_items")
@Getter
@Setter
@NoArgsConstructor
public class ExperimentGuideItem {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Enumerated(EnumType.STRING)
  @Column(name = "scope_type", nullable = false, length = 32)
  private SettingListScopeType scopeType;

  @Column(name = "scope_id", nullable = false)
  private Long scopeId;

  @Enumerated(EnumType.STRING)
  @Column(name = "item_type", nullable = false, length = 32)
  private ExperimentGuideType type;

  @Column(nullable = false, length = 128)
  private String title;

  /** 图片访问路径（兼容旧磁盘存储） */
  @Column(name = "image_url", length = 512)
  private String imageUrl;

  /** 图片二进制数据 */
  @Lob
  @Column(name = "image_data")
  private byte[] imageData;

  /** 图片 MIME 类型 */
  @Column(name = "image_content_type", length = 100)
  private String imageContentType;

  /** 文字说明（IMAGE_TEXT 必填，SETTING_LIST 可选） */
  @Lob
  private String content;

  @Column(name = "sort_order", nullable = false)
  private Integer sortOrder = 0;

  @Column(nullable = false)
  private Boolean enabled = true;

  @Column(name = "show_in_whole_experiment", nullable = false, columnDefinition = "TINYINT(1) NOT NULL DEFAULT 1")
  private Boolean showInWholeExperiment = true;

  @Column(
      name = "created_at",
      nullable = false,
      updatable = false,
      columnDefinition = "TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6)")
  private Instant createdAt = Instant.now();
}
