package com.zeta.business.devicedisplay;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface TerminalOperationTerminalRepository extends JpaRepository<TerminalOperationTerminal, Long> {
    List<TerminalOperationTerminal> findByTerminalOperationIdOrderBySortOrderAscIdAsc(Long terminalOperationId);
    void deleteByTerminalOperationId(Long terminalOperationId);
}
