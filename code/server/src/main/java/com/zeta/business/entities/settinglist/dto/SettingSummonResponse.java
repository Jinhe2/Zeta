package com.zeta.business.entities.settinglist.dto;

import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class SettingSummonResponse {
  private int summonCount;
  private int catalogCount;
  private int matchedCount;
  private List<SettingListItemResponse> items;
}
