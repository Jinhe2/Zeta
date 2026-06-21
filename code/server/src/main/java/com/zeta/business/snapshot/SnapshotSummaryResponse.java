package com.zeta.business.snapshot;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.time.Instant;

@Getter
@AllArgsConstructor
public class SnapshotSummaryResponse {

    private Long id;
    private Long logicId;
    private String logicCode;
    private String logicName;
    private Integer totalTransitions;
    private String status;
    private String source;
    private Instant createdAt;
    private Instant completedAt;
}
