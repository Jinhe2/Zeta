package com.zeta.business.cabinetdisplay;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.time.Instant;

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
