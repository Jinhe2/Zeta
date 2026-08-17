package com.zeta.business.entities.settinglist.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class SettingCheckItemResponse {
  private String settingRef;
  private String settingName;
  private String valueType;
  private String baselineValue;
  private String actualValue;
  private String valueUnit;
  private boolean matched;
  private boolean equal;
}
