package com.zeta.business.entities.learningpath;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface StudentLearningProgressRepository extends JpaRepository<StudentLearningProgress, Long> {
  List<StudentLearningProgress> findByUserId(Long userId);
  Optional<StudentLearningProgress> findByUserIdAndModuleCode(Long userId, String moduleCode);
}
