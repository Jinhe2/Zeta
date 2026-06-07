package com.zeta.repository;

import com.zeta.domain.ProtectionLogic;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ProtectionLogicRepository extends JpaRepository<ProtectionLogic, Long> {

    List<ProtectionLogic> findByEnabledTrueOrderBySortOrderAscIdAsc();

    List<ProtectionLogic> findByDeviceIdAndEnabledTrueOrderBySortOrderAscIdAsc(Long deviceId);

    List<ProtectionLogic> findByDeviceIdOrderBySortOrderAscIdAsc(Long deviceId);

    Optional<ProtectionLogic> findByCode(String code);

    boolean existsByCode(String code);

    List<ProtectionLogic> findByDeviceIsNull();

    long countByDeviceId(Long deviceId);
}
