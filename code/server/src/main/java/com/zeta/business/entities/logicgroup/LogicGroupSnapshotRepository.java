package com.zeta.business.entities.logicgroup;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface LogicGroupSnapshotRepository extends JpaRepository<LogicGroupSnapshot, Long> {

  List<LogicGroupSnapshot> findByGroupIdOrderByCreatedAtDesc(Long groupId);

  List<LogicGroupSnapshot> findByUserIdAndGroupIdOrderByCreatedAtDesc(Long userId, Long groupId);

  Optional<LogicGroupSnapshot> findByIdAndUserId(Long id, Long userId);
}
