package com.zeta.web.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class DeviceCognitionItemResponse {

    private Long id;
    private String title;
    private String content;
    private int sortOrder;
}
