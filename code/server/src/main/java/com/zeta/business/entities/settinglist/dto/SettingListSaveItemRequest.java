package com.zeta.business.entities.settinglist.dto;

import javax.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class SettingListSaveItemRequest {
  @NotBlank(message = "定值引用不能为空")
  private String settingRef;

  @NotBlank(message = "定值不能为空")
  private String baselineValue;

  private Boolean compareEnabled = true;

  private Integer sortOrder;
}
