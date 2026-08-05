package com.zeta.business.entities.cabinetdisplay.dto;

import com.zeta.business.entities.cabinetdisplay.*;
import java.time.Instant;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class CabinetDisplayItemAdminResponse {

  private Long id;
  private Long cabinetId;
  private String cabinetName;
  private String title;
  private String imageUrl;
  private String content;
  private int sortOrder;
  private boolean enabled;
  private Instant createdAt;
}
