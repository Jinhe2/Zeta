package com.zeta.web.dto;

import com.zeta.domain.UserRole;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class AuthTokenResponse {

    private String accessToken;
    private String refreshToken;
    private long accessExpiresIn;
    private String username;
    private String displayName;
    private UserRole role;
    private String homePath;
}
