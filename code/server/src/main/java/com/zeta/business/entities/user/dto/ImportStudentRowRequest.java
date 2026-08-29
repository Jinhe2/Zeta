package com.zeta.business.entities.user.dto;

import com.zeta.business.entities.user.*;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ImportStudentRowRequest {

    private String studentNo;
    private String username;
    private String displayName;
    private String password;
}
