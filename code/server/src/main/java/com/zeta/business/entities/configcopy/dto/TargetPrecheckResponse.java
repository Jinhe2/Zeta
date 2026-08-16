package com.zeta.business.entities.configcopy.dto;

import com.zeta.business.entities.configcopy.ConfigCopyModule;
import com.zeta.business.entities.configcopy.ConfigCopyStatus;
import java.util.List;
import java.util.Map;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class TargetPrecheckResponse {
  private Long targetId;
  private String targetName;
  private ConfigCopyStatus status;
  private List<ConfigCopyIssueResponse> issues;
  private List<ResolvedDeviceMappingResponse> deviceMappings;
  private Map<ConfigCopyModule, Integer> sourceCounts;
  private Map<ConfigCopyModule, Integer> overwriteCounts;
}
