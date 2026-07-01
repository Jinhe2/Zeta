package com.zeta.screen.wiring;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface TerminalWiringCriterionPairRepository extends JpaRepository<TerminalWiringCriterionPair, Long> {

    List<TerminalWiringCriterionPair> findByCriterionIdOrderByIdAsc(Long criterionId);
}
