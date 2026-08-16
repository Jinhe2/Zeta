package com.zeta.business.entities.configcopy.dto;

import java.util.ArrayList;
import java.util.List;
import javax.validation.Valid;
import javax.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class TargetCopyRequest {
  @NotNull private Long targetId;
  @Valid private List<DeviceMappingRequest> deviceMappings = new ArrayList<>();
}
