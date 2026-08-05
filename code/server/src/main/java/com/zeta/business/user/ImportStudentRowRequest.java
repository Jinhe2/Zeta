package com.zeta.business.user;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ImportStudentRowRequest {

    private String username;
    private String displayName;
    private String password;
}
