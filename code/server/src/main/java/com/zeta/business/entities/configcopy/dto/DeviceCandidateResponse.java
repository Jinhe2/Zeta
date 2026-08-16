package com.zeta.business.entities.configcopy.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class DeviceCandidateResponse {
  private Long id;
  private String code;
  private String name;
  private String deviceType;
}
