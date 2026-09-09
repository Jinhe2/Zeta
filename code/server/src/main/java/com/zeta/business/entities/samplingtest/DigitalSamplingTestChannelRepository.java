package com.zeta.business.entities.samplingtest;

import java.util.Collection;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface DigitalSamplingTestChannelRepository
    extends JpaRepository<DigitalSamplingTestChannel, Long> {
  List<DigitalSamplingTestChannel> findBySamplingTestItemIdOrderBySortOrderAscIdAsc(Long itemId);
  void deleteBySamplingTestItemId(Long itemId);
  void deleteBySamplingTestItemIdIn(Collection<Long> itemIds);
}
