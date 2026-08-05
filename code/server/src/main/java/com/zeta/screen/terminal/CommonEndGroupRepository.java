package com.zeta.screen.terminal;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CommonEndGroupRepository extends JpaRepository<CommonEndGroup, Long> {

    List<CommonEndGroup> findByCabinetIdOrderByIdAsc(Long cabinetId);

    List<CommonEndGroup> findByTerminalStripIdOrderByIdAsc(Long terminalStripId);
}
