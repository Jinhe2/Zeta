package com.zeta.business.entities.user.dto;

import com.zeta.business.entities.user.*;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class StudentImportResult {

    private final int rowNumber;
    private final String username;
    private final boolean success;
    private final String message;
}
