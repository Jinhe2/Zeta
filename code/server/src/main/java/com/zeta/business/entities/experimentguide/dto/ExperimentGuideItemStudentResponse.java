package com.zeta.business.entities.experimentguide.dto;

import com.zeta.business.entities.experimentguide.ExperimentGuideType;
import com.zeta.business.entities.settinglist.dto.SettingListItemResponse;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class ExperimentGuideItemStudentResponse {

  private Long id;
  private ExperimentGuideType type;
  private String title;
  private String content;
  private boolean hasImage;
  private int sortOrder;
  /** 仅 SETTING_LIST 类型填充：该作用域生效的定值清单项。 */
  private List<SettingListItemResponse> settingItems;
}
