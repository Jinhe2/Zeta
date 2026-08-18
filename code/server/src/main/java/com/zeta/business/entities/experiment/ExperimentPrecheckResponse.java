package com.zeta.business.entities.experiment;

import com.zeta.business.entities.hardpressboardlist.HardPressboardDtos;
import com.zeta.business.entities.settinglist.dto.SettingCheckResponse;
import com.zeta.business.entities.softpressboardlist.SoftPressboardDtos.CheckResponse;
import com.zeta.business.entities.wiringrequirement.WiringRequirementDtos;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class ExperimentPrecheckResponse {
  private String status;
  private SettingCheckResponse settingCheck;
  private CheckResponse softPressboardCheck;
  private HardPressboardDtos.CheckResponse hardPressboardCheck;
  private WiringRequirementDtos.CheckResponse wiringCheck;
}
