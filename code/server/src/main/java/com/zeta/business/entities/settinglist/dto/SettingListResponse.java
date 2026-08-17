package com.zeta.business.entities.settinglist.dto;

import com.zeta.business.entities.settinglist.SettingListScopeType;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class SettingListResponse {
  private SettingListScopeType scopeType;
  private Long scopeId;
  private String scopeName;
  private Long iedDeviceId;
  private String iedName;
  private SettingListScopeType effectiveScopeType;
  private Long effectiveScopeId;
  private boolean fallbackToDevice;
  private List<SettingListItemResponse> configuredItems;
  private List<SettingListItemResponse> effectiveItems;
}
