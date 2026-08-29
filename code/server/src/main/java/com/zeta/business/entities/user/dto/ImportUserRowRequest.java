package com.zeta.business.entities.user.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ImportUserRowRequest {

    private String studentNo;
    private String username;
    private String displayName;
    private String password;
}
