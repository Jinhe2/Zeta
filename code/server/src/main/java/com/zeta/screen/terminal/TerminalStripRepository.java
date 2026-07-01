package com.zeta.screen.terminal;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface TerminalStripRepository extends JpaRepository<TerminalStrip, Long> {

    List<TerminalStrip> findByCabinetIdOrderBySortOrderAsc(Long cabinetId);
}
