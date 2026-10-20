package com.zeta.business.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.zeta.business.entities.snapshot.LogicSnapshotRepository;
import com.zeta.screen.logicdiagram.ProtectionLogicRepository;
import com.zeta.screen.logicdiagram.SectionSnapshotResponse;
import java.util.List;
import org.junit.jupiter.api.Test;

class LogicSnapshotServiceTest {

  private final LogicSnapshotService service = new LogicSnapshotService(
      mock(LogicSnapshotRepository.class),
      mock(ProtectionLogicRepository.class),
      new ObjectMapper());

  @Test
  void 应将数值空值和空单位映射到对应断面() {
    String json = "{\"nodes\":[{\"id\":\"analog\"},{\"id\":\"zero\"}],"
        + "\"timestamps\":[\"2026/10/20 10:00:00.000\",\"2026/10/20 10:00:00.100\"],"
        + "\"channels\":["
        + "{\"values\":[0,1],\"effectiveValues\":[null,12.3412],"
        + "\"effectiveValueType\":\"fundamental_rms\",\"unit\":\"A\"},"
        + "{\"values\":[1,1],\"effectiveValues\":[0.0,null],"
        + "\"effectiveValueType\":\"fundamental_rms\",\"unit\":\"\"}]}";

    List<SectionSnapshotResponse> sections = service.parseSections(json);

    assertThat(sections).hasSize(2);
    assertThat(sections.get(0).getEffectiveValues().get("analog").getValue()).isNull();
    assertThat(sections.get(1).getEffectiveValues().get("analog").getValue())
        .isEqualTo(12.3412);
    assertThat(sections.get(1).getEffectiveValues().get("analog").getUnit()).isEqualTo("A");
    assertThat(sections.get(0).getEffectiveValues().get("zero").getUnit()).isEmpty();
    assertThat(sections.get(1).getEffectiveValues().get("zero").getValue()).isNull();
  }

  @Test
  void 旧快照应返回空有效值映射() {
    String json = "{\"nodes\":[{\"id\":\"input\"}],"
        + "\"timestamps\":[\"2026/10/20 10:00:00.000\"],"
        + "\"channels\":[{\"values\":[1]}]}";

    List<SectionSnapshotResponse> sections = service.parseSections(json);

    assertThat(sections).hasSize(1);
    assertThat(sections.get(0).getEffectiveValues()).isEmpty();
  }
}
