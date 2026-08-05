package com.zeta.business.entities.cabinetdisplay.dto;

import com.zeta.business.entities.cabinetdisplay.*;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class CabinetDisplayItemResponse {

  private Long id;
  private String title;
  private String imageUrl;
  private String content;
  private int sortOrder;
}
