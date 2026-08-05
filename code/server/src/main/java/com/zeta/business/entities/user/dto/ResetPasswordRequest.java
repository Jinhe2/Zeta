package com.zeta.business.entities.user.dto;

import com.zeta.business.entities.user.*;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ResetPasswordRequest {

    @NotBlank(message = "请输入新密码")
    @Size(min = 6, max = 128, message = "密码长度为 6-128 个字符")
    private String password;
}
