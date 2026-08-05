package com.zeta.business.entities.learningresource;

import com.zeta.business.entities.learningresource.dto.*;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface LearningResourceRepository extends JpaRepository<LearningResource, Long> {
  List<LearningResource> findAllByOrderByUpdatedAtDescIdDesc();

  boolean existsByFilePath(String filePath);
}
