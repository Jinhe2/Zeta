package com.zeta.business.entities.user.dto;

import com.zeta.business.entities.user.*;
import com.zeta.business.entities.user.UserRole;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class UpdateUserRequest {

    @Size(max = 64, message = "学号不能超过 64 个字符")
    private String studentNo;

    @NotBlank(message = "请输入显示名称")
    @Size(max = 64, message = "显示名称不能超过 64 个字符")
    private String displayName;

    @NotNull(message = "请选择用户角色")
    private UserRole role;
}
