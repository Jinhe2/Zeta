package com.zeta.business.entities.logicgroup;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface LogicGroupRepository extends JpaRepository<LogicGroup, Long> {

  List<LogicGroup> findByIedDeviceIdOrderBySortOrderAscIdAsc(Long iedDeviceId);
}
