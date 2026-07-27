package com.zeta.business.drawinglearning;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;

public interface DrawingPageRepository extends JpaRepository<DrawingPage, Long> {
    List<DrawingPage> findByDrawingGroupIdOrderBySortOrderAscIdAsc(Long drawingGroupId);
    List<DrawingPage> findByDrawingGroupIdAndEnabledTrueOrderBySortOrderAscIdAsc(Long drawingGroupId);
    List<DrawingPage> findByDrawingGroupIdIn(Collection<Long> drawingGroupIds);
    long countByDrawingGroupIdAndEnabledTrue(Long drawingGroupId);
    void deleteByDrawingGroupId(Long drawingGroupId);
}
