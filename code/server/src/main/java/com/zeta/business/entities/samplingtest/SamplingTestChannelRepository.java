package com.zeta.business.entities.samplingtest;

import java.util.Collection;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SamplingTestChannelRepository extends JpaRepository<SamplingTestChannel, Long> {
  List<SamplingTestChannel> findBySamplingTestItemIdOrderBySortOrderAscIdAsc(Long itemId);
  void deleteBySamplingTestItemId(Long itemId);
  void deleteBySamplingTestItemIdIn(Collection<Long> itemIds);
}
