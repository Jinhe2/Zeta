package com.zeta.screen.terminal;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TerminalStripRepository extends JpaRepository<TerminalStrip, Long> {

    List<TerminalStrip> findByCabinetIdOrderBySortOrderAsc(Long cabinetId);
}
