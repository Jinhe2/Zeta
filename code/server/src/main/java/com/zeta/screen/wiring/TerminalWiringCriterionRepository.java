package com.zeta.screen.wiring;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface TerminalWiringCriterionRepository extends JpaRepository<TerminalWiringCriterion, Long> {

    List<TerminalWiringCriterion> findByTerminalIdOrderBySortOrderAsc(Long terminalId);

    List<TerminalWiringCriterion> findByWiringDetectionDeviceIdOrderByIdAsc(Long wiringDetectionDeviceId);
}
