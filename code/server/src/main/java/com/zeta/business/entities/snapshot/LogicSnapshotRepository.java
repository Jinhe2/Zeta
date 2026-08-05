package com.zeta.business.entities.snapshot;

import com.zeta.business.entities.snapshot.dto.*;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface LogicSnapshotRepository extends JpaRepository<LogicSnapshot, Long> {

    List<LogicSnapshot> findByUserIdOrderByCreatedAtDesc(Long userId);

    List<LogicSnapshot> findByUserIdAndLogicIdOrderByCreatedAtDesc(Long userId, Long logicId);

    Optional<LogicSnapshot> findByIdAndUserId(Long id, Long userId);

    Optional<LogicSnapshot> findFirstByUserIdAndLogicIdOrderByCreatedAtDesc(Long userId, Long logicId);
}
