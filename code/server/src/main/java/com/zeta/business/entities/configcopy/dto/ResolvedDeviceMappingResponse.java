package com.zeta.business.entities.configcopy.dto;

import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class ResolvedDeviceMappingResponse {
  private Long sourceDeviceId;
  private String sourceDeviceCode;
  private String sourceDeviceName;
  private Long targetDeviceId;
  private String targetDeviceCode;
  private String targetDeviceName;
  private boolean automatic;
  private List<DeviceCandidateResponse> candidates;
}
