package com.zeta.web.dto;

import com.zeta.domain.UserRole;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class UserProfileResponse {

    private String username;
    private String displayName;
    private UserRole role;
    private String homePath;
}
