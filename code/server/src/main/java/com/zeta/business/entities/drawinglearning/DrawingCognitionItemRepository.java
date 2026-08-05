package com.zeta.business.entities.drawinglearning;

import java.util.Collection;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface DrawingCognitionItemRepository extends JpaRepository<DrawingCognitionItem, Long> {
  List<DrawingCognitionItem> findByDrawingPageIdOrderBySortOrderAscIdAsc(Long drawingPageId);

  List<DrawingCognitionItem> findByDrawingPageIdAndEnabledTrueOrderBySortOrderAscIdAsc(
      Long drawingPageId);

  List<DrawingCognitionItem> findByDrawingPageIdIn(Collection<Long> drawingPageIds);

  long countByDrawingPageIdInAndEnabledTrue(Collection<Long> drawingPageIds);

  void deleteByDrawingPageId(Long drawingPageId);

  void deleteByDrawingPageIdIn(Collection<Long> drawingPageIds);
}
