package com.zeta.screen.wiring;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TerminalWiringCriterionRepository extends JpaRepository<TerminalWiringCriterion, Long> {

    List<TerminalWiringCriterion> findByTerminalIdOrderBySortOrderAsc(Long terminalId);

    List<TerminalWiringCriterion> findByWiringDetectionDeviceIdOrderByIdAsc(Long wiringDetectionDeviceId);
}
