package com.zeta.screen.knowledge;

import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class KnowledgeTreeResponse {

    private List<CabinetTreeNodeResponse> cabinets;
}
