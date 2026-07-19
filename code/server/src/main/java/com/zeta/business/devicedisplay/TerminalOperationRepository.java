package com.zeta.business.devicedisplay;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface TerminalOperationRepository extends JpaRepository<TerminalOperation, Long> {
    Optional<TerminalOperation> findByDeviceDisplayItemId(Long deviceDisplayItemId);
    void deleteByDeviceDisplayItemId(Long deviceDisplayItemId);
}
