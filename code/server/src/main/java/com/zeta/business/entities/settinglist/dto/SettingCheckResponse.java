package com.zeta.business.entities.settinglist.dto;

import com.zeta.business.entities.settinglist.SettingListScopeType;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class SettingCheckResponse {
  private String status;
  private SettingListScopeType effectiveScopeType;
  private Long effectiveScopeId;
  private int total;
  private int equal;
  private int mismatch;
  private int missing;
  private List<SettingCheckItemResponse> items;
}
