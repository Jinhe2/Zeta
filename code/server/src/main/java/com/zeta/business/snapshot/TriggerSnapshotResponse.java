package com.zeta.business.snapshot;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class TriggerSnapshotResponse {

    private Long id;
    private String status;
    private Integer totalTransitions;
    private String message;
}
