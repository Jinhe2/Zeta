package com.zeta.business.entities.drawinglearning;

import com.zeta.business.entities.drawinglearning.dto.*;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface DrawingGroupRepository extends JpaRepository<DrawingGroup, Long> {
  List<DrawingGroup> findByScreenCabinetIdOrderByDrawingTypeAscSortOrderAscIdAsc(
      Long screenCabinetId);

  List<DrawingGroup> findByScreenCabinetIdAndEnabledTrueOrderByDrawingTypeAscSortOrderAscIdAsc(
      Long screenCabinetId);
}
