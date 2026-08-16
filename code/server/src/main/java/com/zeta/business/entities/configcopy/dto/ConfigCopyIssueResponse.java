package com.zeta.business.entities.configcopy.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class ConfigCopyIssueResponse {
  private String code;
  private String message;
  private Long sourceId;
}
