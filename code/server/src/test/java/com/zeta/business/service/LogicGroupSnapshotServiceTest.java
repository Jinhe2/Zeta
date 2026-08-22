package com.zeta.business.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.zeta.business.entities.logicgroup.LogicGroupMember;
import com.zeta.business.entities.logicgroup.LogicGroupMemberRepository;
import com.zeta.business.entities.logicgroup.LogicGroupRepository;
import com.zeta.business.entities.logicgroup.LogicGroupSnapshot;
import com.zeta.business.entities.logicgroup.LogicGroupSnapshotRepository;
import com.zeta.business.entities.logicgroup.dto.LogicGroupSnapshotDtos.MemberDetailResponse;
import com.zeta.business.entities.logicgroup.dto.LogicGroupSnapshotDtos.MemberSummaryResponse;
import com.zeta.business.entities.snapshot.LogicSnapshotRepository;
import com.zeta.screen.logicdiagram.ProtectionLogicRepository;
import java.io.InputStream;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

class LogicGroupSnapshotServiceTest {

  private static final Long USER_ID = 7L;
  private static final Long SNAPSHOT_ID = 25L;
  private static final Long GROUP_ID = 3L;

  private final ObjectMapper objectMapper = new ObjectMapper();
  private LogicGroupSnapshotRepository snapshotRepository;
  private LogicGroupMemberRepository memberRepository;
  private String fixtureJson;

  @BeforeEach
  void setUp() throws Exception {
    snapshotRepository = mock(LogicGroupSnapshotRepository.class);
    memberRepository = mock(LogicGroupMemberRepository.class);
    try (InputStream input = getClass().getResourceAsStream(
        "/snapshots/logic-group-snapshot-v3.json")) {
      fixtureJson = objectMapper.readTree(input).toString();
    }
    when(memberRepository.findByGroupIdOrderBySortOrderAscIdAsc(GROUP_ID))
        .thenReturn(Arrays.asList(member(1L, 0), member(12L, 1), member(16L, 2)));
  }

  @Test
  void 应返回三个成员摘要并保持顺序和变位数() {
    LogicGroupSnapshotService service = serviceFor(fixtureJson, realParser());

    List<MemberSummaryResponse> members = service.listMembers(USER_ID, SNAPSHOT_ID);

    assertThat(members).extracting(MemberSummaryResponse::getLogicDiagramId)
        .containsExactly(1L, 12L, 16L);
    assertThat(members).extracting(MemberSummaryResponse::getOrder)
        .containsExactly(0, 1, 2);
    assertThat(members).extracting(MemberSummaryResponse::getStatus)
        .containsOnly("success");
    assertThat(members).extracting(MemberSummaryResponse::getTotalTransitions)
        .containsExactly(4, 16, 5);
  }

  @Test
  void 应按成员变位点生成五个十七个和六个独立断面() throws Exception {
    LogicGroupSnapshotService service = serviceFor(fixtureJson, realParser());
    JsonNode root = objectMapper.readTree(fixtureJson);

    MemberDetailResponse first = service.getMember(USER_ID, SNAPSHOT_ID, 1L);
    MemberDetailResponse second = service.getMember(USER_ID, SNAPSHOT_ID, 12L);
    MemberDetailResponse third = service.getMember(USER_ID, SNAPSHOT_ID, 16L);

    assertThat(first.getSections()).hasSize(5);
    assertThat(second.getSections()).hasSize(17);
    assertThat(third.getSections()).hasSize(6);
    assertThat(first.getSections().get(1).getTimestamp())
        .isEqualTo(root.path("timestamps").get(1).asText());
    assertThat(first.getSections().get(1).getStates().get("input_1")).isTrue();
    assertThat(second.getSections().get(13).getTimestamp())
        .isEqualTo(root.path("timestamps").get(17).asText());
    assertThat(third.getSections().get(5).getTimestamp())
        .isEqualTo(root.path("timestamps").get(20).asText());
  }

  @Test
  void 应按相同索引裁剪定时器耗时数组() throws Exception {
    LogicSnapshotService parser = mock(LogicSnapshotService.class);
    final String[] normalized = new String[1];
    when(parser.parseSections(anyString())).thenAnswer(invocation -> {
      normalized[0] = invocation.getArgument(0);
      return Collections.emptyList();
    });
    LogicGroupSnapshotService service = serviceFor(fixtureJson, parser);

    service.getMember(USER_ID, SNAPSHOT_ID, 12L);

    JsonNode memberSnapshot = objectMapper.readTree(normalized[0]);
    JsonNode timer = null;
    for (JsonNode channel : memberSnapshot.path("channels")) {
      if (channel.has("elapsed")) {
        timer = channel;
        break;
      }
    }
    assertThat(timer).isNotNull();
    assertThat(timer.path("elapsed")).hasSize(17);
    assertThat(timer.path("elapsed").get(8).asInt()).isEqualTo(87000);
  }

  @Test
  void 失败成员应返回空断面和业务错误() throws Exception {
    JsonNode root = objectMapper.readTree(fixtureJson);
    com.fasterxml.jackson.databind.node.ObjectNode member =
        (com.fasterxml.jackson.databind.node.ObjectNode) root.path("logics").get(0);
    member.put("status", "failed");
    member.putNull("experimentPassed");
    member.put("totalTransitions", 0);
    member.put("errorCode", "CALCULATION_FAILED");
    member.put("errorMessage", "基础逻辑计算失败");
    member.remove(Arrays.asList("nodes", "channels", "changedPointIndices"));
    LogicGroupSnapshotService service = serviceFor(root.toString(), realParser());

    MemberDetailResponse result = service.getMember(USER_ID, SNAPSHOT_ID, 1L);

    assertThat(result.getStatus()).isEqualTo("failed");
    assertThat(result.getErrorCode()).isEqualTo("CALCULATION_FAILED");
    assertThat(result.getErrorMessage()).isEqualTo("基础逻辑计算失败");
    assertThat(result.getSections()).isEmpty();
  }

  @Test
  void 重复变位索引应去重且仍生成正确断面() throws Exception {
    JsonNode root = objectMapper.readTree(fixtureJson);
    com.fasterxml.jackson.databind.node.ArrayNode indices =
        (com.fasterxml.jackson.databind.node.ArrayNode) root.path("logics").get(0)
            .path("changedPointIndices");
    indices.add(1);
    indices.add(0);
    LogicGroupSnapshotService service = serviceFor(root.toString(), realParser());

    MemberDetailResponse result = service.getMember(USER_ID, SNAPSHOT_ID, 1L);

    assertThat(result.getSections()).hasSize(5);
  }

  @Test
  void 未生成预期断面且零次变位应识别为装置未启动() {
    LogicGroupSnapshotService service = serviceFor(fixtureJson, realParser());
    LogicGroupSnapshot snapshot = new LogicGroupSnapshot();
    snapshot.setSnapshotJson("{\"resultType\":\"diagnosis\"}");
    snapshot.setTotalTransitions(0);

    assertThat(service.resolveResultStatus(snapshot)).isEqualTo("DEVICE_NOT_STARTED");

    snapshot.setTotalTransitions(2);
    assertThat(service.resolveResultStatus(snapshot)).isEqualTo("INVALID_SNAPSHOT");

    snapshot.setSnapshotJson(fixtureJson);
    assertThat(service.resolveResultStatus(snapshot)).isEqualTo("SNAPSHOT_READY");
  }

  @Test
  void 非本组合成员和其他用户快照应返回四零四() {
    LogicGroupSnapshotService service = serviceFor(fixtureJson, realParser());

    assertStatus(() -> service.getMember(USER_ID, SNAPSHOT_ID, 99L), HttpStatus.NOT_FOUND);
    assertStatus(() -> service.get(USER_ID + 1, SNAPSHOT_ID), HttpStatus.NOT_FOUND);
  }

  @Test
  void 损坏版本越界索引和通道长度错误应返回四二二() throws Exception {
    JsonNode wrongVersion = objectMapper.readTree(fixtureJson);
    ((com.fasterxml.jackson.databind.node.ObjectNode) wrongVersion).put("version", "2.3");
    assertStatus(
        () -> serviceFor(wrongVersion.toString(), realParser()).listMembers(USER_ID, SNAPSHOT_ID),
        HttpStatus.UNPROCESSABLE_ENTITY);

    JsonNode outOfRange = objectMapper.readTree(fixtureJson);
    ((com.fasterxml.jackson.databind.node.ArrayNode) outOfRange.path("logics").get(0)
        .path("changedPointIndices")).set(0, objectMapper.getNodeFactory().numberNode(999));
    assertStatus(
        () -> serviceFor(outOfRange.toString(), realParser()).listMembers(USER_ID, SNAPSHOT_ID),
        HttpStatus.UNPROCESSABLE_ENTITY);

    JsonNode shortChannel = objectMapper.readTree(fixtureJson);
    ((com.fasterxml.jackson.databind.node.ArrayNode) shortChannel.path("logics").get(0)
        .path("channels").get(0).path("values")).remove(0);
    assertStatus(
        () -> serviceFor(shortChannel.toString(), realParser()).listMembers(USER_ID, SNAPSHOT_ID),
        HttpStatus.UNPROCESSABLE_ENTITY);

    JsonNode wrongNodeIndex = objectMapper.readTree(fixtureJson);
    ((com.fasterxml.jackson.databind.node.ObjectNode) wrongNodeIndex.path("logics").get(0)
        .path("channels").get(0)).put("nodeIndex", 1);
    assertStatus(
        () -> serviceFor(wrongNodeIndex.toString(), realParser()).listMembers(USER_ID, SNAPSHOT_ID),
        HttpStatus.UNPROCESSABLE_ENTITY);

    JsonNode wrongTransitions = objectMapper.readTree(fixtureJson);
    ((com.fasterxml.jackson.databind.node.ObjectNode) wrongTransitions.path("logics").get(0))
        .put("totalTransitions", 99);
    assertStatus(
        () -> serviceFor(wrongTransitions.toString(), realParser()).listMembers(USER_ID, SNAPSHOT_ID),
        HttpStatus.UNPROCESSABLE_ENTITY);
  }

  private LogicGroupSnapshotService serviceFor(String json, LogicSnapshotService parser) {
    LogicGroupSnapshot snapshot = new LogicGroupSnapshot();
    snapshot.setId(SNAPSHOT_ID);
    snapshot.setUserId(USER_ID);
    snapshot.setGroupId(GROUP_ID);
    snapshot.setSnapshotJson(json);
    when(snapshotRepository.findByIdAndUserId(SNAPSHOT_ID, USER_ID))
        .thenReturn(Optional.of(snapshot));
    return new LogicGroupSnapshotService(
        snapshotRepository,
        mock(LogicGroupRepository.class),
        memberRepository,
        parser,
        objectMapper);
  }

  private LogicSnapshotService realParser() {
    return new LogicSnapshotService(
        mock(LogicSnapshotRepository.class),
        mock(ProtectionLogicRepository.class),
        objectMapper);
  }

  private LogicGroupMember member(Long logicDiagramId, int order) {
    LogicGroupMember member = new LogicGroupMember();
    member.setGroupId(GROUP_ID);
    member.setLogicDiagramId(logicDiagramId);
    member.setSortOrder(order);
    return member;
  }

  private void assertStatus(Runnable action, HttpStatus status) {
    assertThatThrownBy(action::run)
        .isInstanceOfSatisfying(ResponseStatusException.class,
            error -> assertThat(error.getStatus()).isEqualTo(status));
  }
}
