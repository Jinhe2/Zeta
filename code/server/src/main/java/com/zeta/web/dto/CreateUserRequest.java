package com.zeta.web.dto;

import com.zeta.domain.UserRole;
import lombok.Getter;
import lombok.Setter;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;

@Getter
@Setter
public class CreateUserRequest {

    @NotBlank(message = "请输入用户名")
    @Size(max = 64, message = "用户名不能超过 64 个字符")
    private String username;

    @NotBlank(message = "请输入密码")
    @Size(min = 6, max = 128, message = "密码长度为 6-128 个字符")
    private String password;

    @NotBlank(message = "请输入显示名称")
    @Size(max = 64, message = "显示名称不能超过 64 个字符")
    private String displayName;

    @NotNull(message = "请选择用户角色")
    private UserRole role;
}
