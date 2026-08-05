package com.zeta.business.entities.binding.dto;

import com.zeta.business.entities.binding.*;
import java.time.Instant;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class BindingListResponse {

    private Long cabinetId;
    private String cabinetName;
    private String cabinetLocation;
    /** null = 未绑定 */
    private String bindId;
    private String bindLabel;
    private Instant boundAt;
}
