package com.zeta.business.entities.user.dto;

import com.zeta.business.entities.user.*;
import com.zeta.business.entities.user.UserRole;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class UserProfileResponse {

    private String username;
    private String studentNo;
    private String displayName;
    private UserRole role;
    private String homePath;
}
