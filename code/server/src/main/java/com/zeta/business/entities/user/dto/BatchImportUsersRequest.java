package com.zeta.business.entities.user.dto;

import java.util.List;
import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class BatchImportUsersRequest {

    @NotEmpty(message = "导入数据不能为空")
    @Size(max = 500, message = "单次最多导入 500 名用户")
    private List<ImportUserRowRequest> users;
}
