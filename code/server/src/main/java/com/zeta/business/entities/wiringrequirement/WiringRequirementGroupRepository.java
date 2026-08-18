package com.zeta.business.entities.wiringrequirement;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface WiringRequirementGroupRepository
    extends JpaRepository<WiringRequirementGroup, Long> {
  List<WiringRequirementGroup> findByConfigIdOrderByGroupNoAscIdAsc(Long configId);

  void deleteByConfigId(Long configId);
}
