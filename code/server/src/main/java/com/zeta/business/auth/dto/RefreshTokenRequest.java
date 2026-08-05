package com.zeta.business.auth.dto;

import com.zeta.business.auth.*;
import javax.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class RefreshTokenRequest {

    @NotBlank
    private String refreshToken;
}
