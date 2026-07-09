package com.zeta.business.logicnodecognition;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface LogicNodeCognitionItemRepository extends JpaRepository<LogicNodeCognitionItem, Long> {

    List<LogicNodeCognitionItem> findByLogicDiagramIdAndNodeIdOrderBySortOrderAscIdAsc(
            Long logicDiagramId,
            String nodeId);

    long countByLogicDiagramIdAndNodeId(Long logicDiagramId, String nodeId);
}
