package com.zeta.screen.logicdiagram;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ProtectionLogicRepository extends JpaRepository<ProtectionLogic, Long> {

    List<ProtectionLogic> findAllByOrderByIdAsc();

    List<ProtectionLogic> findByDeviceIdOrderByIdAsc(Long deviceId);

    List<ProtectionLogic> findByDeviceCabinetIdOrderByIdAsc(Long cabinetId);

    Optional<ProtectionLogic> findByDeviceIdAndLogicId(Long deviceId, String logicId);

    boolean existsByDeviceIdAndLogicId(Long deviceId, String logicId);

    long countByDeviceId(Long deviceId);
}
