package com.zeta.business.entities.user.dto;

import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class BatchImportUsersResponse {

    private final int successCount;
    private final int failureCount;
    private final List<UserImportResult> results;
}
