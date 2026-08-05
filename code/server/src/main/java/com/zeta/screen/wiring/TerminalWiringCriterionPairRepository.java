package com.zeta.screen.wiring;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TerminalWiringCriterionPairRepository extends JpaRepository<TerminalWiringCriterionPair, Long> {

    List<TerminalWiringCriterionPair> findByCriterionIdOrderByIdAsc(Long criterionId);
}
