package com.zeta.repository;

import com.zeta.domain.Device;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface DeviceRepository extends JpaRepository<Device, Long> {

    Optional<Device> findByCode(String code);

    List<Device> findByCabinetIdAndEnabledTrueOrderBySortOrderAscIdAsc(Long cabinetId);

    List<Device> findByCabinetIdOrderBySortOrderAscIdAsc(Long cabinetId);

    long countByCabinetId(Long cabinetId);

    boolean existsByCode(String code);
}
