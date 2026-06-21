package com.zeta.business.snapshot;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.time.Instant;

@Getter
@AllArgsConstructor
public class SnapshotDetailResponse {

    private Long id;
    private Long logicId;
    private String logicCode;
    private String logicName;
    private Integer totalTransitions;
    private String status;
    private String errorMessage;
    private Instant createdAt;
    private Instant completedAt;
    private String snapshotJson;
}
