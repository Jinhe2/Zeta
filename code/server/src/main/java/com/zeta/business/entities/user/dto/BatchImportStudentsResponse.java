package com.zeta.business.entities.user.dto;

import com.zeta.business.entities.user.*;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class BatchImportStudentsResponse {

    private final int successCount;
    private final int failureCount;
    private final List<StudentImportResult> results;
}
