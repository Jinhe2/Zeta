package com.zeta.business.entities.logiclearning;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface LogicLearningConfigRepository extends JpaRepository<LogicLearningConfig, Long> {

  Optional<LogicLearningConfig> findByLogicDiagramId(Long logicDiagramId);

  List<LogicLearningConfig> findByLogicDiagramIdIn(Collection<Long> logicDiagramIds);
}
