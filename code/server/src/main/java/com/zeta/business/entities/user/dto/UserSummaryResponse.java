package com.zeta.business.entities.user.dto;

import com.zeta.business.entities.user.*;
import com.zeta.business.entities.user.UserRole;
import java.time.Instant;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class UserSummaryResponse {

    private Long id;
    private String username;
    private String studentNo;
    private String displayName;
    private UserRole role;
    private Instant createdAt;
}
