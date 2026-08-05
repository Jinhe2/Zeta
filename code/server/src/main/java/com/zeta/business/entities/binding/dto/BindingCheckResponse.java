package com.zeta.business.entities.binding.dto;

import com.zeta.business.entities.binding.*;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class BindingCheckResponse {

    /** BOUND / UNBOUND */
    private String status;
    private Long cabinetId;
    private String cabinetName;
    private String bindId;
    private String bindLabel;
}
