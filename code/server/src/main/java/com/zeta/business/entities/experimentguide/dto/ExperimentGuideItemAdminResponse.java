package com.zeta.business.entities.experimentguide.dto;

import com.zeta.business.entities.experimentguide.ExperimentGuideType;
import com.zeta.business.entities.settinglist.SettingListScopeType;
import java.time.Instant;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class ExperimentGuideItemAdminResponse {

  private Long id;
  private SettingListScopeType scopeType;
  private Long scopeId;
  private ExperimentGuideType type;
  private String title;
  private boolean hasImage;
  private String content;
  private int sortOrder;
  private boolean enabled;
  private Instant createdAt;
}
