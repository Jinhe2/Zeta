package com.zeta.web.dto;

import com.zeta.domain.UserRole;
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
