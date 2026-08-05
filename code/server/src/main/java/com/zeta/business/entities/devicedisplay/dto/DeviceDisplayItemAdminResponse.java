package com.zeta.business.entities.devicedisplay.dto;

import com.zeta.business.entities.devicedisplay.*;
import com.zeta.business.media.CognitionMediaType;
import java.time.Instant;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class DeviceDisplayItemAdminResponse {

  private Long id;
  private Long cognitionDeviceId;
  private String cognitionDeviceTitle;
  private String title;
  private String imageUrl;
  private CognitionMediaType mediaType;
  private String videoPath;
  private Double leftPercent;
  private Double topPercent;
  private Double widthPercent;
  private Double heightPercent;
  private String content;
  private int sortOrder;
  private boolean enabled;
  private Instant createdAt;
  private TerminalOperationResponse terminalOperation;
}
