package com.zeta.business.entities.wholeexperiment;

import java.util.*;
import javax.persistence.LockModeType;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;

public interface WholeExperimentRunRepository extends JpaRepository<WholeExperimentRun, String> {
  Optional<WholeExperimentRun> findByTaskUuidAndUserId(String taskUuid, Long userId);
  List<WholeExperimentRun> findByExperimentIdAndUserIdOrderByCreatedAtDesc(Long experimentId, Long userId);
  boolean existsByExperimentIdAndStatusIn(Long experimentId, Collection<String> statuses);

  @Lock(LockModeType.PESSIMISTIC_WRITE)
  @Query("select r from WholeExperimentRun r where r.taskUuid=:id")
  Optional<WholeExperimentRun> lockById(@Param("id") String id);
}
