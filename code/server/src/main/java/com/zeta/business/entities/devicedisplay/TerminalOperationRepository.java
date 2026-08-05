package com.zeta.business.entities.devicedisplay;

import com.zeta.business.entities.devicedisplay.dto.*;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TerminalOperationRepository extends JpaRepository<TerminalOperation, Long> {
  Optional<TerminalOperation> findByDeviceDisplayItemId(Long deviceDisplayItemId);

  void deleteByDeviceDisplayItemId(Long deviceDisplayItemId);
}
