package com.zeta.screen.baseline;

import lombok.AllArgsConstructor;
import lombok.Getter;

/** 学员端只读展示的 IED 基准定值。 */
@Getter
@AllArgsConstructor
public class IedBaselineSettingResponse {
    private String description;
    private String baselineValue;
}
