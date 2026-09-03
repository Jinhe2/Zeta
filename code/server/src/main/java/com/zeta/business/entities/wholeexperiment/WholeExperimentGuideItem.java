package com.zeta.business.entities.wholeexperiment;

import com.zeta.business.entities.experimentguide.dto.ExperimentGuideItemStudentResponse;
import com.zeta.business.entities.settinglist.dto.SettingListItemResponse;
import java.util.List;
import lombok.Getter;

/** 引导保留原条目 ID 供图片读取，并标明它来自哪个基础逻辑。 */
@Getter
public class WholeExperimentGuideItem extends ExperimentGuideItemStudentResponse {
  private final Long sourceLogicDiagramId;

  public WholeExperimentGuideItem(ExperimentGuideItemStudentResponse item, Long sourceLogicDiagramId,
      List<SettingListItemResponse> settingItems) {
    super(item.isShowInWholeExperiment(), item.getId(), item.getType(), item.getTitle(),
        item.getContent(), item.isHasImage(), item.getSortOrder(), settingItems);
    this.sourceLogicDiagramId = sourceLogicDiagramId;
  }
}
