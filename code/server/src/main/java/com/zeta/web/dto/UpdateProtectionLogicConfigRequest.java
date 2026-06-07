package com.zeta.web.dto;

import lombok.Getter;
import lombok.Setter;

import javax.validation.constraints.NotBlank;

@Getter
@Setter
public class UpdateProtectionLogicConfigRequest {

    @NotBlank(message = "配置内容不能为空")
    private String configJson;
}
