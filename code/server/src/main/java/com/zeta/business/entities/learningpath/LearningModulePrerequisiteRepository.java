package com.zeta.business.entities.learningpath;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface LearningModulePrerequisiteRepository extends JpaRepository<LearningModulePrerequisite, Long> {
  List<LearningModulePrerequisite> findAllByOrderByIdAsc();
  void deleteByModuleCode(String moduleCode);
}
