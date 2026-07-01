package com.zeta.screen.terminal;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface TerminalRepository extends JpaRepository<Terminal, Long> {

    List<Terminal> findByCabinetIdOrderByIdAsc(Long cabinetId);

    List<Terminal> findByTerminalStripIdOrderByIdAsc(Long terminalStripId);

    List<Terminal> findByIedDeviceIdOrderByIdAsc(Long iedDeviceId);
}
