package com.zeta.business.entities.cabinetdisplay;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CabinetDisplayItemRepository extends JpaRepository<CabinetDisplayItem, Long> {
  boolean existsByImageUrl(String imageUrl);

  Optional<CabinetDisplayItem> findByScreenCabinetIdAndTitle(Long screenCabinetId, String title);

  List<CabinetDisplayItem> findByScreenCabinetIdOrderBySortOrderAscIdAsc(Long screenCabinetId);

  void deleteByScreenCabinetId(Long screenCabinetId);
}
