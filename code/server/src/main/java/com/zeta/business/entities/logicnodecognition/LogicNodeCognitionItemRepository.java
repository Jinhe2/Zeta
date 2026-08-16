package com.zeta.business.entities.logicnodecognition;

import java.util.Collection;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface LogicNodeCognitionItemRepository
    extends JpaRepository<LogicNodeCognitionItem, Long> {
  boolean existsByImageUrl(String imageUrl);
  boolean existsByVideoPath(String videoPath);

  List<LogicNodeCognitionItem> findByLogicDiagramIdAndNodeIdOrderBySortOrderAscIdAsc(
      Long logicDiagramId, String nodeId);

  long countByLogicDiagramIdAndNodeId(Long logicDiagramId, String nodeId);

  List<LogicNodeCognitionItem> findByLogicDiagramIdIn(Collection<Long> logicDiagramIds);

  void deleteByLogicDiagramIdIn(Collection<Long> logicDiagramIds);
}
