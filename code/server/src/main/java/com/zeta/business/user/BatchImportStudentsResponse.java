package com.zeta.business.user;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.List;

@Getter
@AllArgsConstructor
public class BatchImportStudentsResponse {

    private final int successCount;
    private final int failureCount;
    private final List<StudentImportResult> results;
}
