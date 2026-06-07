package com.zeta.web.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.List;

@Getter
@AllArgsConstructor
public class KnowledgeTreeResponse {

    private List<CabinetTreeNodeResponse> cabinets;
}
