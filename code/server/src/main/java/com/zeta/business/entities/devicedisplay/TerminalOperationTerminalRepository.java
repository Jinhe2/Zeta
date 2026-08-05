package com.zeta.business.entities.devicedisplay;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TerminalOperationTerminalRepository
    extends JpaRepository<TerminalOperationTerminal, Long> {
  List<TerminalOperationTerminal> findByTerminalOperationIdOrderBySortOrderAscIdAsc(
      Long terminalOperationId);

  void deleteByTerminalOperationId(Long terminalOperationId);
}
