package com.zeta.screen.virtualcircuit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.*;
import javax.sql.DataSource;
import org.junit.jupiter.api.Test;

class VirtualCircuitTopologyServiceTest {

  @Test
  void aggregatesControlBlocksByDirectedPortPair() {
    VirtualCircuitTopologyService service =
        new VirtualCircuitTopologyService(mock(DataSource.class), new ObjectMapper());
    List<Map<String, Object>> blocks = Arrays.asList(
        block(11L, "IED-A", "1-A", "IED-B", "2-A"),
        block(12L, "IED-A", "1-A", "IED-B", "2-A"),
        block(13L, "IED-B", "2-A", "IED-A", "1-A"));

    List<Map<String, Object>> edges = service.aggregateEdges(blocks);

    assertThat(edges).hasSize(2);
    assertThat(castIds(edges.get(0).get("controlBlockIds"))).containsExactly(11L, 12L);
    assertThat(edges.get(1)).containsEntry("sourceIedName", "IED-B")
        .containsEntry("targetIedName", "IED-A");
  }

  @Test
  void keepsBlockWhenPortsAreNotConfigured() {
    VirtualCircuitTopologyService service =
        new VirtualCircuitTopologyService(mock(DataSource.class), new ObjectMapper());
    Map<String, Object> block = block(21L, "IED-A", null, "IED-B", null);

    Map<String, Object> edge = service.aggregateEdges(Collections.singletonList(block)).get(0);

    assertThat(edge).containsEntry("sourcePort", VirtualCircuitTopologyService.UNCONFIGURED_PORT)
        .containsEntry("targetPort", VirtualCircuitTopologyService.UNCONFIGURED_PORT);
  }

  @Test
  void resolvesDisplayNameByConfiguredDescriptionThenVirtualDescriptionThenIedName() {
    VirtualCircuitTopologyService service =
        new VirtualCircuitTopologyService(mock(DataSource.class), new ObjectMapper());

    assertThat(service.displayName(
        "IED-A", Collections.singletonList(device("IED-A", "装置表描述")), "虚端子表描述"))
        .isEqualTo("装置表描述");
    assertThat(service.displayName(
        "IED-B", Collections.singletonList(device("IED-B", "")), "虚端子表描述"))
        .isEqualTo("虚端子表描述");
    assertThat(service.displayName("IED-C", Collections.emptyList(), null)).isEqualTo("IED-C");
  }

  @Test
  void keepsFirstNonBlankVirtualDescription() {
    VirtualCircuitTopologyService service =
        new VirtualCircuitTopologyService(mock(DataSource.class), new ObjectMapper());
    Map<String, String> descriptions = new LinkedHashMap<>();

    service.rememberDescription(descriptions, "IED-A", "");
    service.rememberDescription(descriptions, "IED-A", "第一条描述");
    service.rememberDescription(descriptions, "IED-A", "第二条描述");

    assertThat(descriptions).containsEntry("IED-A", "第一条描述");
  }

  private Map<String, Object> block(
      Long id, String source, String sourcePort, String target, String targetPort) {
    Map<String, Object> block = new LinkedHashMap<>();
    block.put("id", id);
    block.put("sourceIedName", source);
    block.put("receiverIedName", target);
    block.put("sourcePorts", sourcePort == null ? Collections.emptyList() : Collections.singletonList(sourcePort));
    block.put("targetPorts", targetPort == null ? Collections.emptyList() : Collections.singletonList(targetPort));
    return block;
  }

  private Map<String, Object> device(String iedName, String description) {
    Map<String, Object> device = new LinkedHashMap<>();
    device.put("iedName", iedName);
    device.put("description", description);
    device.put("displayName", description == null || description.trim().isEmpty()
        ? iedName : description);
    return device;
  }

  @SuppressWarnings("unchecked")
  private List<Long> castIds(Object value) {
    return (List<Long>) value;
  }
}
