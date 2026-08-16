package com.zeta.business.entities.samplingtest;

import java.math.BigDecimal;
import javax.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "sampling_test_channels")
@Getter
@Setter
@NoArgsConstructor
public class SamplingTestChannel {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(name = "sampling_test_item_id", nullable = false)
  private Long samplingTestItemId;

  @Column(name = "output_code", nullable = false, length = 2)
  private String outputCode;

  @Column(name = "terminal_id", nullable = false)
  private Long terminalId;

  @Column(name = "baseline_magnitude", precision = 18, scale = 6)
  private BigDecimal baselineMagnitude;

  @Column(name = "baseline_angle", precision = 12, scale = 6)
  private BigDecimal baselineAngle;

  @Column(name = "sort_order", nullable = false)
  private Integer sortOrder;
}
