package com.zeta.business.entities.wiringrequirement;

import java.util.Collection;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface WiringRequirementGroupRepository
    extends JpaRepository<WiringRequirementGroup, Long> {
  List<WiringRequirementGroup> findByConfigIdOrderByGroupNoAscIdAsc(Long configId);

  List<WiringRequirementGroup> findByConfigIdIn(Collection<Long> configIds);

  void deleteByConfigId(Long configId);

  void deleteByConfigIdIn(Collection<Long> configIds);
}
