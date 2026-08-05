package com.zeta.business.entities.snapshot.dto;

import com.zeta.business.entities.snapshot.*;
import java.time.Instant;
import lombok.AllArgsConstructor;
import lombok.Getter;

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
