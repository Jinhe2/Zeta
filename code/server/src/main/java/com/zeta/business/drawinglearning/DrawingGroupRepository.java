package com.zeta.business.drawinglearning;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface DrawingGroupRepository extends JpaRepository<DrawingGroup, Long> {
    List<DrawingGroup> findByScreenCabinetIdOrderByDrawingTypeAscSortOrderAscIdAsc(Long screenCabinetId);
    List<DrawingGroup> findByScreenCabinetIdAndEnabledTrueOrderByDrawingTypeAscSortOrderAscIdAsc(Long screenCabinetId);
}
