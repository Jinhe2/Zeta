package com.zeta.business.entities.learningpath;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface LearningModuleRepository extends JpaRepository<LearningModule, String> {
  List<LearningModule> findAllByOrderBySortOrderAscCodeAsc();
  List<LearningModule> findByEnabledTrueOrderBySortOrderAscCodeAsc();
}
