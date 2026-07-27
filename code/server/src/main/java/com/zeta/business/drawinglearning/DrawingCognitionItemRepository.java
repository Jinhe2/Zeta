package com.zeta.business.drawinglearning;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;

public interface DrawingCognitionItemRepository extends JpaRepository<DrawingCognitionItem, Long> {
    List<DrawingCognitionItem> findByDrawingPageIdOrderBySortOrderAscIdAsc(Long drawingPageId);
    List<DrawingCognitionItem> findByDrawingPageIdAndEnabledTrueOrderBySortOrderAscIdAsc(Long drawingPageId);
    List<DrawingCognitionItem> findByDrawingPageIdIn(Collection<Long> drawingPageIds);
    long countByDrawingPageIdInAndEnabledTrue(Collection<Long> drawingPageIds);
    void deleteByDrawingPageId(Long drawingPageId);
    void deleteByDrawingPageIdIn(Collection<Long> drawingPageIds);
}
