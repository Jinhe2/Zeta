package com.zeta.business.entities.settinglist.dto;

import java.util.ArrayList;
import java.util.List;
import javax.validation.Valid;
import javax.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class SettingListSaveRequest {
  @NotNull(message = "定值清单不能为空")
  @Valid
  private List<SettingListSaveItemRequest> items = new ArrayList<>();
}
