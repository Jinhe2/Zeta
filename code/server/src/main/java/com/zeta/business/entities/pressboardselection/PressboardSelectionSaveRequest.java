package com.zeta.business.entities.pressboardselection;

import java.util.List;
import javax.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class PressboardSelectionSaveRequest {
  @NotNull(message = "请选择需要校验的压板项目，可提交空数组")
  private List<String> pressboardRefs;
}
