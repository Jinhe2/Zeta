package com.zeta.screen.logicdiagram;

import com.zeta.screen.logicdiagram.ProtectionLogic;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ProtectionLogicRepository extends JpaRepository<ProtectionLogic, Long> {

    List<ProtectionLogic> findAllByOrderByIdAsc();

    List<ProtectionLogic> findByDeviceIdOrderByIdAsc(Long deviceId);

    Optional<ProtectionLogic> findByDeviceIdAndLogicId(Long deviceId, String logicId);

    boolean existsByDeviceIdAndLogicId(Long deviceId, String logicId);

    long countByDeviceId(Long deviceId);
}
