package com.zeta.screen.softpressboard;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface IedSoftPressboardItemRepository
    extends JpaRepository<IedSoftPressboardItem, Long> {
  List<IedSoftPressboardItem> findByIedDeviceIdOrderByIdAsc(Long iedDeviceId);
}
