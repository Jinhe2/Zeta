package com.zeta.business.entities.configcopy.dto;

import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class ConfigCopyPrecheckResponse {
  private boolean ready;
  private List<TargetPrecheckResponse> targets;
}
