package com.zeta.business.user;

import lombok.Getter;
import lombok.Setter;

import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.Size;
import java.util.List;

@Getter
@Setter
public class BatchImportStudentsRequest {

    @NotEmpty(message = "导入数据不能为空")
    @Size(max = 500, message = "单次最多导入 500 名学员")
    private List<ImportStudentRowRequest> students;
}
