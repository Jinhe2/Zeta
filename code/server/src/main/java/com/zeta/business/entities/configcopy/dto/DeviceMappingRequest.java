package com.zeta.business.entities.configcopy.dto;

import javax.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class DeviceMappingRequest {
  @NotNull private Long sourceDeviceId;
  @NotNull private Long targetDeviceId;
}
