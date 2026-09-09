package com.zeta.business.entities.samplingtest;

import java.math.BigDecimal;
import javax.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** 数字化采样测试通道，保存屏柜库采样配置的业务快照。 */
@Entity
@Table(name = "digital_sampling_test_channels")
@Getter
@Setter
@NoArgsConstructor
public class DigitalSamplingTestChannel {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(name = "sampling_test_item_id", nullable = false)
  private Long samplingTestItemId;

  @Column(name = "sampling_signal_association_id", nullable = false)
  private Long samplingSignalAssociationId;

  @Column(name = "sampling_signal_channel_id", nullable = false)
  private Long samplingSignalChannelId;

  @Column(name = "ied_device_id", nullable = false)
  private Long iedDeviceId;

  @Column(nullable = false, length = 16)
  private String category;

  @Column(name = "control_identity_key", nullable = false, length = 64)
  private String controlIdentityKey;

  @Column(name = "source_ied_name", length = 128)
  private String sourceIedName;

  @Column(name = "control_name", length = 128)
  private String controlName;

  @Column(name = "control_reference", length = 512)
  private String controlReference;

  @Column(name = "telemetry_reference", nullable = false, length = 768)
  private String telemetryReference;

  @Column(name = "telemetry_key", nullable = false, length = 64)
  private String telemetryKey;

  @Column(name = "telemetry_description", length = 512)
  private String telemetryDescription;

  @Column(name = "dataset_name", length = 128)
  private String datasetName;

  @Column(name = "value_type", length = 64)
  private String valueType;

  @Column(name = "baseline_magnitude", nullable = false, precision = 18, scale = 6)
  private BigDecimal baselineMagnitude;

  @Column(name = "baseline_angle", nullable = false, precision = 12, scale = 6)
  private BigDecimal baselineAngle;

  @Column(name = "sort_order", nullable = false)
  private Integer sortOrder;
}
