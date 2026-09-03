package com.zeta.business.entities.wholeexperiment;

import java.time.Instant;
import javax.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "whole_experiment",
    indexes = @Index(name = "idx_whole_experiment_recent", columnList = "user_id,device_id,last_started_at"),
    uniqueConstraints = @UniqueConstraint(
    name = "uk_whole_experiment_owner", columnNames = {"user_id", "device_id", "member_signature"}))
@Getter
@Setter
public class WholeExperiment {
  @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;
  @Column(name = "user_id", nullable = false) private Long userId;
  @Column(name = "device_id", nullable = false) private Long deviceId;
  @Column(name = "member_signature", nullable = false, length = 128) private String memberSignature;
  @Column(name = "last_started_at") private Instant lastStartedAt;
  @Column(name = "created_at", nullable = false) private Instant createdAt = Instant.now();
}
