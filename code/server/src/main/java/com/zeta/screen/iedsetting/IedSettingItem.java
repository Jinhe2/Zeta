package com.zeta.screen.iedsetting;

import java.time.Instant;
import javax.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** IED 全量 SG 定值目录，对应 ct-screen.ied_setting_item。 */
@Entity
@Table(name = "ied_setting_item")
@Getter
@Setter
@NoArgsConstructor
public class IedSettingItem {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(name = "ied_device_id", nullable = false)
  private Long iedDeviceId;

  @Column(name = "setting_name", nullable = false, length = 256)
  private String settingName;

  @Column(name = "setting_ref", nullable = false, length = 512)
  private String settingRef;

  @Column(name = "value_type", nullable = false, length = 16)
  private String valueType;

  @Column(name = "created_at", nullable = false, updatable = false)
  private Instant createdAt;
}
