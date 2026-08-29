package com.zeta.business.entities.user.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class UserImportResult {

    private final int rowNumber;
    private final String username;
    private final String studentNo;
    private final boolean success;
    private final String message;
}
