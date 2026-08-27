package com.zeta.business.entities.settinglist.dto;

import com.zeta.business.entities.settinglist.SettingListItem;
import com.zeta.business.entities.settinglist.SettingValueTypePolicy;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class SettingListItemResponse {
  private String settingRef;
  private String settingFc;
  private String settingName;
  private String valueType;
  private boolean compareEnabled;
  private String baselineValue;
  private Integer sortOrder;

  public static SettingListItemResponse from(SettingListItem item) {
    return new SettingListItemResponse(
        item.getSettingRef(),
        item.getSettingFc(),
        item.getSettingName(),
        SettingValueTypePolicy.effectiveType(item.getSettingRef(), item.getValueType()),
        !Boolean.FALSE.equals(item.getCompareEnabled()),
        item.getBaselineValue(),
        item.getSortOrder());
  }
}
