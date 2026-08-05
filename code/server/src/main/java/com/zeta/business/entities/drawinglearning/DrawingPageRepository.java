package com.zeta.business.entities.drawinglearning;

import java.util.Collection;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface DrawingPageRepository extends JpaRepository<DrawingPage, Long> {
  List<DrawingPage> findByDrawingGroupIdOrderBySortOrderAscIdAsc(Long drawingGroupId);

  List<DrawingPage> findByDrawingGroupIdAndEnabledTrueOrderBySortOrderAscIdAsc(Long drawingGroupId);

  List<DrawingPage> findByDrawingGroupIdIn(Collection<Long> drawingGroupIds);

  long countByDrawingGroupIdAndEnabledTrue(Long drawingGroupId);

  void deleteByDrawingGroupId(Long drawingGroupId);
}
