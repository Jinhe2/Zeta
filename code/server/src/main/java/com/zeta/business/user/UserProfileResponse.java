package com.zeta.business.user;

import com.zeta.business.user.UserRole;
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
