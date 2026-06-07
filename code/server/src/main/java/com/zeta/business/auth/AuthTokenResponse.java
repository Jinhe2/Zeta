package com.zeta.business.auth;

import com.zeta.business.user.UserRole;
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
