package com.zeta.business.user;

import com.zeta.business.user.UserRole;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.time.Instant;

@Getter
@AllArgsConstructor
public class UserSummaryResponse {

    private Long id;
    private String username;
    private String displayName;
    private UserRole role;
    private Instant createdAt;
}
