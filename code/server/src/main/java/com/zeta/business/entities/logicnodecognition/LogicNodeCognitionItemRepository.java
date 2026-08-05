package com.zeta.business.entities.logicnodecognition;

import com.zeta.business.entities.logicnodecognition.dto.*;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface LogicNodeCognitionItemRepository
    extends JpaRepository<LogicNodeCognitionItem, Long> {
  boolean existsByVideoPath(String videoPath);

  List<LogicNodeCognitionItem> findByLogicDiagramIdAndNodeIdOrderBySortOrderAscIdAsc(
      Long logicDiagramId, String nodeId);

  long countByLogicDiagramIdAndNodeId(Long logicDiagramId, String nodeId);
}
