package com.zeta.business.entities.snapshot.dto;

import com.zeta.business.entities.snapshot.*;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ImportSnapshotRequest {
    private String snapshotJson;
}
