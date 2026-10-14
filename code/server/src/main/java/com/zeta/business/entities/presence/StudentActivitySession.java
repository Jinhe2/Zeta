package com.zeta.business.entities.presence;

import java.time.Instant;
import javax.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "student_activity_sessions", indexes = {
    @Index(name = "idx_sas_user_status", columnList = "user_id,status"),
    @Index(name = "idx_sas_cabinet_time", columnList = "cabinet_id,started_at,expires_at"),
    @Index(name = "idx_sas_device_status", columnList = "device_bind_id,status")
})
@Getter
@Setter
@NoArgsConstructor
public class StudentActivitySession {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(name = "user_id", nullable = false)
  private Long userId;

  @Column(name = "cabinet_id", nullable = false)
  private Long cabinetId;

  @Column(name = "cabinet_name_snapshot", length = 128)
  private String cabinetNameSnapshot;

  @Column(name = "device_bind_id", nullable = false, length = 64)
  private String deviceBindId;

  @Column(name = "bind_label_snapshot", length = 128)
  private String bindLabelSnapshot;

  @Column(name = "started_at", nullable = false)
  private Instant startedAt;

  @Column(name = "last_activity_at", nullable = false)
  private Instant lastActivityAt;

  @Column(name = "expires_at", nullable = false)
  private Instant expiresAt;

  @Column(name = "ended_at")
  private Instant endedAt;

  @Column(name = "duration_seconds", nullable = false)
  private long durationSeconds;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = 32)
  private StudentActivityStatus status;
}
