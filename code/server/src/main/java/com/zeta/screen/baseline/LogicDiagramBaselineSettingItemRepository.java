package com.zeta.screen.baseline;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface LogicDiagramBaselineSettingItemRepository extends JpaRepository<LogicDiagramBaselineSettingItem, Long> {

    List<LogicDiagramBaselineSettingItem> findByLogicDiagramIdOrderBySortOrderAsc(Long logicDiagramId);
}
