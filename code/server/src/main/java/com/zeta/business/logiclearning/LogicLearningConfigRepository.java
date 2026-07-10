package com.zeta.business.logiclearning;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface LogicLearningConfigRepository extends JpaRepository<LogicLearningConfig, Long> {

    Optional<LogicLearningConfig> findByLogicDiagramId(Long logicDiagramId);

    List<LogicLearningConfig> findByLogicDiagramIdIn(Collection<Long> logicDiagramIds);
}
