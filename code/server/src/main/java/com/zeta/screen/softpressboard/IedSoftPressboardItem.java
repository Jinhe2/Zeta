package com.zeta.screen.softpressboard;

import java.time.Instant;
import javax.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** IED 软压板目录项，对应只读屏柜库 ied_soft_pressboard_item。 */
@Entity
@Table(name = "ied_soft_pressboard_item")
@Getter
@Setter
@NoArgsConstructor
public class IedSoftPressboardItem {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(name = "ied_device_id", nullable = false)
  private Long iedDeviceId;

  @Column(name = "pressboard_name", nullable = false, length = 256)
  private String pressboardName;

  @Column(name = "pressboard_ref", nullable = false, length = 512)
  private String pressboardRef;

  @Column(name = "created_at", nullable = false, updatable = false)
  private Instant createdAt;
}
