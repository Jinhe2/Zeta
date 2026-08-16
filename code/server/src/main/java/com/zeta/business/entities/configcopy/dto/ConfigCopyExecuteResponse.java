package com.zeta.business.entities.configcopy.dto;

import com.zeta.business.entities.configcopy.ConfigCopyModule;
import java.util.List;
import java.util.Map;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class ConfigCopyExecuteResponse {
  private boolean success;
  private ConfigCopyPrecheckResponse precheck;
  private List<TargetExecutionResponse> targets;

  @Getter
  @AllArgsConstructor
  public static class TargetExecutionResponse {
    private Long targetId;
    private String targetName;
    private Map<ConfigCopyModule, Integer> copiedCounts;
  }
}
