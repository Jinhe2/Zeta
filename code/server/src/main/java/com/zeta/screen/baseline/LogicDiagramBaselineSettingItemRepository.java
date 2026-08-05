package com.zeta.screen.baseline;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface LogicDiagramBaselineSettingItemRepository extends JpaRepository<LogicDiagramBaselineSettingItem, Long> {

    List<LogicDiagramBaselineSettingItem> findByLogicDiagramIdOrderBySortOrderAsc(Long logicDiagramId);
}
