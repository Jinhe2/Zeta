package com.zeta.business.entities.logicgroup;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface LogicGroupMemberRepository extends JpaRepository<LogicGroupMember, Long> {

  List<LogicGroupMember> findByGroupIdOrderBySortOrderAscIdAsc(Long groupId);

  void deleteByGroupId(Long groupId);

  long countByGroupId(Long groupId);
}
