package com.zeta.business.cabinetdisplay;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface CabinetDisplayItemRepository extends JpaRepository<CabinetDisplayItem, Long> {

    Optional<CabinetDisplayItem> findByScreenCabinetIdAndTitle(Long screenCabinetId, String title);

    List<CabinetDisplayItem> findByScreenCabinetIdOrderBySortOrderAscIdAsc(Long screenCabinetId);
}
