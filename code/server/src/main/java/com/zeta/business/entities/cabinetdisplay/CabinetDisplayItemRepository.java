package com.zeta.business.entities.cabinetdisplay;

import com.zeta.business.entities.cabinetdisplay.dto.*;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CabinetDisplayItemRepository extends JpaRepository<CabinetDisplayItem, Long> {

  Optional<CabinetDisplayItem> findByScreenCabinetIdAndTitle(Long screenCabinetId, String title);

  List<CabinetDisplayItem> findByScreenCabinetIdOrderBySortOrderAscIdAsc(Long screenCabinetId);
}
