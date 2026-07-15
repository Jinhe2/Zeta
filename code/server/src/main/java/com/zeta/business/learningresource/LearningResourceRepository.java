package com.zeta.business.learningresource;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface LearningResourceRepository extends JpaRepository<LearningResource, Long> {
    List<LearningResource> findAllByOrderByUpdatedAtDescIdDesc();

    boolean existsByFilePath(String filePath);
}
