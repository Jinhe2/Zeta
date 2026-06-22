package com.zeta.business.binding;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.time.Instant;

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
