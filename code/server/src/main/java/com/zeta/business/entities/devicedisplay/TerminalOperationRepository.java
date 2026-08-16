package com.zeta.business.entities.devicedisplay;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface TerminalOperationRepository extends JpaRepository<TerminalOperation, Long> {
  Optional<TerminalOperation> findByDeviceDisplayItemId(Long deviceDisplayItemId);

  List<TerminalOperation> findByDeviceDisplayItemIdIn(Collection<Long> deviceDisplayItemIds);

  void deleteByDeviceDisplayItemIdIn(Collection<Long> deviceDisplayItemIds);

  void deleteByDeviceDisplayItemId(Long deviceDisplayItemId);

  @Modifying
  @Query(
      value =
          "INSERT INTO terminal_operation_items (device_display_item_id, terminal_strip_id) "
              + "VALUES (:deviceDisplayItemId, :terminalStripId) "
              + "ON DUPLICATE KEY UPDATE terminal_strip_id = VALUES(terminal_strip_id)",
      nativeQuery = true)
  void upsertByDeviceDisplayItemId(
      @Param("deviceDisplayItemId") Long deviceDisplayItemId,
      @Param("terminalStripId") Long terminalStripId);
}
