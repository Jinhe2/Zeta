package com.zeta.screen.ieddevice;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface DeviceRepository extends JpaRepository<Device, Long> {

    Optional<Device> findByIedName(String iedName);

    Optional<Device> findByCabinetIdAndIedName(Long cabinetId, String iedName);

    List<Device> findByCabinetIdOrderByIdAsc(Long cabinetId);

    long countByCabinetId(Long cabinetId);

    boolean existsByIedName(String iedName);

    boolean existsByCabinetIdAndIedName(Long cabinetId, String iedName);
}
