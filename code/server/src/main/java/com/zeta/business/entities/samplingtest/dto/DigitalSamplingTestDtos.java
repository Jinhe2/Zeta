package com.zeta.business.entities.samplingtest.dto;

import java.math.BigDecimal;
import java.util.List;
import javax.validation.Valid;
import javax.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** 数字化采样配置的请求和响应模型。 */
public final class DigitalSamplingTestDtos {
  private DigitalSamplingTestDtos() {}

  @Getter
  @Setter
  @NoArgsConstructor
  public static class ConfigRequest {
    @NotNull(message = "请选择装置")
    private Long iedDeviceId;
    @Valid
    private List<ChannelRequest> channels;
  }

  @Getter
  @Setter
  @NoArgsConstructor
  public static class ChannelRequest {
    @NotNull(message = "请选择采样信号通道")
    private Long samplingSignalChannelId;
    @NotNull(message = "请输入基准幅值")
    private BigDecimal baselineMagnitude;
    @NotNull(message = "请输入基准相角")
    private BigDecimal baselineAngle;
  }

  @Getter
  @AllArgsConstructor
  public static class ConfigResponse {
    private Long iedDeviceId;
    private String iedName;
    private String deviceName;
    private boolean valid;
    private String invalidReason;
    private List<ChannelResponse> channels;
  }

  @Getter
  @AllArgsConstructor
  public static class ChannelResponse {
    private Long samplingSignalAssociationId;
    private Long samplingSignalChannelId;
    private String category;
    private String controlIdentityKey;
    private String sourceIedName;
    private String controlName;
    private String controlReference;
    private String telemetryReference;
    private String telemetryKey;
    private String telemetryDescription;
    private String datasetName;
    private String valueType;
    private BigDecimal baselineMagnitude;
    private BigDecimal baselineAngle;
    private int sortOrder;
    private boolean valid;
    private String invalidReason;
  }
}
