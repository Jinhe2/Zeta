package com.zeta.business.cognitiondevice;

import lombok.Getter;
import lombok.Setter;

import javax.validation.constraints.Max;
import javax.validation.constraints.Min;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;

@Getter
@Setter
public class CreateCognitionDeviceRequest {

    @NotNull(message = "请选择设备类型")
    private CognitionDeviceType deviceType;

    private Long screenDeviceId;

    @NotBlank(message = "请输入设备名称")
    @Size(max = 128)
    private String title;

    @NotNull
    @Min(0)
    @Max(100)
    private Double leftPercent;

    @NotNull
    @Min(0)
    @Max(100)
    private Double topPercent;

    @NotNull
    @Min(0)
    @Max(100)
    private Double widthPercent;

    @NotNull
    @Min(0)
    @Max(100)
    private Double heightPercent;

    @NotNull
    private Integer sortOrder;

    private Boolean enabled = true;
}
