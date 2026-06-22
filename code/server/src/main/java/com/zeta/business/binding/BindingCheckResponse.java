package com.zeta.business.binding;

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
