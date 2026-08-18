package com.zeta.business.entities.wiringrequirement;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface WiringRequirementConfigRepository
    extends JpaRepository<WiringRequirementConfig, Long> {
  List<WiringRequirementConfig> findByLogicDiagramIdOrderByIdAsc(Long logicDiagramId);

  void deleteByLogicDiagramId(Long logicDiagramId);
}
