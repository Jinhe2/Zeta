package com.zeta.screen.hardpressboard;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface HardPressboardRepository extends JpaRepository<HardPressboard, Long> {

    List<HardPressboard> findByCabinetIdOrderByIdAsc(Long cabinetId);

    List<HardPressboard> findByIedDeviceIdOrderByIdAsc(Long iedDeviceId);
}
