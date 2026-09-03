package com.zeta.business.entities.settinglist.dto;

import java.util.List;
import javax.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class SettingSelectionSaveRequest {
  @NotNull(message = "请选择需要校验的定值项目，可提交空数组")
  private List<String> settingRefs;
}
