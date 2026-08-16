package com.zeta.business.entities.samplingtest;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SamplingTestItemRepository extends JpaRepository<SamplingTestItem, Long> {
  List<SamplingTestItem> findByScreenCabinetIdOrderBySortOrderAscIdAsc(Long cabinetId);
  boolean existsByVideoPath(String videoPath);
  void deleteByScreenCabinetId(Long cabinetId);
}
