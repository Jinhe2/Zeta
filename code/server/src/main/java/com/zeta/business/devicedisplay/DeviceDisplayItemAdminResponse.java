package com.zeta.business.devicedisplay;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.time.Instant;
import com.zeta.business.media.CognitionMediaType;

@Getter
@AllArgsConstructor
public class DeviceDisplayItemAdminResponse {

    private Long id;
    private Long cognitionDeviceId;
    private String cognitionDeviceTitle;
    private String title;
    private String imageUrl;
    private CognitionMediaType mediaType;
    private String videoPath;
    private Double leftPercent;
    private Double topPercent;
    private Double widthPercent;
    private Double heightPercent;
    private String content;
    private int sortOrder;
    private boolean enabled;
    private Instant createdAt;
}
