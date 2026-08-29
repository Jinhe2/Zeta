package com.zeta.business.auth.dto;

import com.zeta.business.auth.*;
import com.zeta.business.entities.user.UserRole;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class AuthTokenResponse {

    private String accessToken;
    private String refreshToken;
    private long accessExpiresIn;
    private String username;
    private String studentNo;
    private String displayName;
    private UserRole role;
    private String homePath;
}
