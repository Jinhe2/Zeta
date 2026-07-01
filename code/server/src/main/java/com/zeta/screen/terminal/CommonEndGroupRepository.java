package com.zeta.screen.terminal;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface CommonEndGroupRepository extends JpaRepository<CommonEndGroup, Long> {

    List<CommonEndGroup> findByCabinetIdOrderByIdAsc(Long cabinetId);

    List<CommonEndGroup> findByTerminalStripIdOrderByIdAsc(Long terminalStripId);
}
