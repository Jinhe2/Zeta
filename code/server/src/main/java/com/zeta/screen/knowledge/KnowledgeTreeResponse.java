package com.zeta.screen.knowledge;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.List;

@Getter
@AllArgsConstructor
public class KnowledgeTreeResponse {

    private List<CabinetTreeNodeResponse> cabinets;
}
