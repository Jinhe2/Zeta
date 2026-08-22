package com.zeta.business.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.zeta.business.entities.logicgroup.LogicGroup;
import com.zeta.business.entities.logicgroup.LogicGroupMemberRepository;
import com.zeta.business.entities.logicgroup.LogicGroupRepository;
import com.zeta.business.entities.logicgroup.LogicGroupSnapshot;
import com.zeta.business.entities.logicgroup.LogicGroupSnapshotRepository;
import com.zeta.business.entities.logicgroup.dto.LogicGroupSnapshotDtos.MemberDetailResponse;
import com.zeta.business.entities.logicgroup.dto.LogicGroupSnapshotDtos.MemberSummaryResponse;
import com.zeta.screen.logicdiagram.SectionSnapshotResponse;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class LogicGroupSnapshotService {

  private final LogicGroupSnapshotRepository snapshotRepository;
  private final LogicGroupRepository groupRepository;
  private final LogicGroupMemberRepository memberRepository;
  private final LogicSnapshotService logicSnapshotService;
  private final ObjectMapper objectMapper;

  public LogicGroupSnapshotService(
      LogicGroupSnapshotRepository snapshotRepository,
      LogicGroupRepository groupRepository,
      LogicGroupMemberRepository memberRepository,
      LogicSnapshotService logicSnapshotService,
      ObjectMapper objectMapper) {
    this.snapshotRepository = snapshotRepository;
    this.groupRepository = groupRepository;
    this.memberRepository = memberRepository;
    this.logicSnapshotService = logicSnapshotService;
    this.objectMapper = objectMapper;
  }

  @Transactional("businessTransactionManager")
  public LogicGroupSnapshot create(
      Long userId,
      Long groupId,
      String snapshotJson,
      Integer totalTransitions,
      Boolean experimentPassed) {
    LogicGroup group = groupRepository.findById(groupId).orElse(null);
    LogicGroupSnapshot snapshot = new LogicGroupSnapshot();
    snapshot.setUserId(userId);
    snapshot.setGroupId(groupId);
    snapshot.setGroupName(group == null ? null : group.getName());
    snapshot.setSnapshotJson(snapshotJson);
    snapshot.setTotalTransitions(totalTransitions == null ? 0 : totalTransitions);
    snapshot.setExperimentPassed(experimentPassed);
    snapshot.setStatus("COMPLETED");
    snapshot.setSource("MONITOR");
    snapshot.setCompletedAt(Instant.now());
    return snapshotRepository.save(snapshot);
  }

  public List<LogicGroupSnapshot> listByGroup(Long userId, Long groupId) {
    return snapshotRepository.findByUserIdAndGroupIdOrderByCreatedAtDesc(userId, groupId);
  }

  public LogicGroupSnapshot get(Long userId, Long snapshotId) {
    return snapshotRepository
        .findByIdAndUserId(snapshotId, userId)
        .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "组合实验结果不存在"));
  }

  public String resolveResultStatus(LogicGroupSnapshot snapshot) {
    try {
      JsonNode root = objectMapper.readTree(snapshot.getSnapshotJson());
      if (root != null
          && root.isObject()
          && "3.0".equals(root.path("version").asText())
          && "logic_group_snapshot_v1".equals(root.path("resultType").asText())
          && root.path("timestamps").isArray()
          && root.path("timestamps").size() > 0
          && root.path("sampleIndices").isArray()
          && root.path("sampleIndices").size() == root.path("timestamps").size()
          && root.path("logics").isArray()
          && root.path("logics").size() > 0) {
        return "SNAPSHOT_READY";
      }
    } catch (Exception ignored) {
      // 未生成预期组合断面时，由历史摘要转换为用户可理解的结果状态。
    }
    return snapshot.getTotalTransitions() == null || snapshot.getTotalTransitions() == 0
        ? "DEVICE_NOT_STARTED"
        : "INVALID_SNAPSHOT";
  }

  public List<MemberSummaryResponse> listMembers(Long userId, Long snapshotId) {
    LogicGroupSnapshot snapshot = get(userId, snapshotId);
    JsonNode root = parseAndValidateRoot(snapshot);
    Set<Long> groupMemberIds = new HashSet<>();
    memberRepository.findByGroupIdOrderBySortOrderAscIdAsc(snapshot.getGroupId())
        .forEach(member -> groupMemberIds.add(member.getLogicDiagramId()));
    List<MemberSummaryResponse> result = new ArrayList<>();
    Set<Long> snapshotMemberIds = new HashSet<>();
    for (JsonNode member : root.path("logics")) {
      MemberSummaryResponse summary = toSummary(member);
      if (!snapshotMemberIds.add(summary.getLogicDiagramId())) {
        throw invalidFormat("组合断面包含重复的 logicDiagramId: " + summary.getLogicDiagramId());
      }
      if (!groupMemberIds.contains(summary.getLogicDiagramId())) {
        throw new ResponseStatusException(HttpStatus.NOT_FOUND, "组合断面包含非本组合成员");
      }
      if (!"failed".equals(summary.getStatus())) {
        buildMemberSnapshotJson(root, member, summary);
      }
      result.add(summary);
    }
    result.sort(Comparator.comparingInt(item -> item.getOrder() == null ? 0 : item.getOrder()));
    return result;
  }

  public MemberDetailResponse getMember(Long userId, Long snapshotId, Long logicDiagramId) {
    LogicGroupSnapshot snapshot = get(userId, snapshotId);
    requireGroupMember(snapshot.getGroupId(), logicDiagramId);
    JsonNode root = parseAndValidateRoot(snapshot);
    JsonNode member = findMember(root, logicDiagramId);
    MemberSummaryResponse summary = toSummary(member);

    List<SectionSnapshotResponse> sections = Collections.emptyList();
    if (!"failed".equalsIgnoreCase(summary.getStatus())) {
      String normalizedJson = buildMemberSnapshotJson(root, member, summary);
      sections = logicSnapshotService.parseSections(normalizedJson);
    }

    return new MemberDetailResponse(
        snapshot.getId(),
        snapshot.getGroupId(),
        summary.getLogicDiagramId(),
        summary.getLogicId(),
        summary.getOrder(),
        summary.getStatus(),
        summary.getExperimentPassed(),
        summary.getTotalTransitions(),
        summary.getErrorCode(),
        summary.getErrorMessage(),
        sections);
  }

  private JsonNode parseAndValidateRoot(LogicGroupSnapshot snapshot) {
    final JsonNode root;
    try {
      root = objectMapper.readTree(snapshot.getSnapshotJson());
    } catch (Exception e) {
      throw invalidFormat("组合断面 JSON 无法解析");
    }
    if (!root.isObject()
        || !"3.0".equals(root.path("version").asText())
        || !"logic_group_snapshot_v1".equals(root.path("resultType").asText())) {
      throw invalidFormat("仅支持 v3.0 logic_group_snapshot_v1 组合断面");
    }
    JsonNode timestamps = root.path("timestamps");
    JsonNode sampleIndices = root.path("sampleIndices");
    JsonNode logics = root.path("logics");
    if (!timestamps.isArray() || timestamps.size() == 0) {
      throw invalidFormat("根级 timestamps 必须是非空数组");
    }
    if (!sampleIndices.isArray() || sampleIndices.size() != timestamps.size()) {
      throw invalidFormat("根级 sampleIndices 必须与 timestamps 等长");
    }
    for (int index = 0; index < timestamps.size(); index++) {
      if (!timestamps.get(index).isTextual()
          || !timestamps.get(index).asText().matches("\\d{4}/\\d{2}/\\d{2} \\d{2}:\\d{2}:\\d{2}\\.\\d{3}")) {
        throw invalidFormat("根级 timestamps 包含无效绝对时标");
      }
      if (!sampleIndices.get(index).isIntegralNumber()
          || !sampleIndices.get(index).canConvertToInt()
          || sampleIndices.get(index).asInt() < 0) {
        throw invalidFormat("根级 sampleIndices 包含无效采样序号");
      }
    }
    if (!logics.isArray()) {
      throw invalidFormat("根级 logics 必须是数组");
    }
    return root;
  }

  private void requireGroupMember(Long groupId, Long logicDiagramId) {
    boolean exists = memberRepository.findByGroupIdOrderBySortOrderAscIdAsc(groupId).stream()
        .anyMatch(member -> logicDiagramId.equals(member.getLogicDiagramId()));
    if (!exists) {
      throw new ResponseStatusException(HttpStatus.NOT_FOUND, "基础逻辑不属于当前组合");
    }
  }

  private JsonNode findMember(JsonNode root, Long logicDiagramId) {
    for (JsonNode member : root.path("logics")) {
      if (member.path("logicDiagramId").canConvertToLong()
          && logicDiagramId.longValue() == member.path("logicDiagramId").asLong()) {
        return member;
      }
    }
    throw new ResponseStatusException(
        HttpStatus.NOT_FOUND, "组合断面中不存在基础逻辑 " + logicDiagramId + " 的结果");
  }

  private MemberSummaryResponse toSummary(JsonNode member) {
    if (!member.isObject()
        || !member.path("logicDiagramId").isIntegralNumber()
        || !member.path("logicDiagramId").canConvertToLong()
        || member.path("logicDiagramId").asLong() <= 0) {
      throw invalidFormat("成员 logicDiagramId 无效");
    }
    String logicId = nullableText(member, "logicId");
    if (logicId == null || logicId.trim().isEmpty()) {
      throw invalidFormat("成员 logicId 不能为空");
    }
    if (!member.has("order") || !member.get("order").isIntegralNumber()
        || !member.get("order").canConvertToInt()
        || member.get("order").asInt() < 0) {
      throw invalidFormat("成员 order 无效");
    }
    String status = nullableText(member, "status");
    if (!"success".equals(status) && !"failed".equals(status)) {
      throw invalidFormat("成员 status 仅支持 success 或 failed");
    }
    if (!member.has("totalTransitions")
        || !member.get("totalTransitions").isIntegralNumber()
        || !member.get("totalTransitions").canConvertToInt()
        || member.get("totalTransitions").asInt() < 0) {
      throw invalidFormat("成员 totalTransitions 无效");
    }
    if ("success".equals(status)
        && (!member.has("experimentPassed") || !member.get("experimentPassed").isBoolean())) {
      throw invalidFormat("成功成员 experimentPassed 必须是布尔值");
    }
    return new MemberSummaryResponse(
        member.path("logicDiagramId").asLong(),
        logicId,
        member.get("order").asInt(),
        status,
        nullableBoolean(member, "experimentPassed"),
        member.get("totalTransitions").asInt(),
        nullableText(member, "errorCode"),
        nullableText(member, "errorMessage"));
  }

  private String buildMemberSnapshotJson(
      JsonNode root, JsonNode member, MemberSummaryResponse summary) {
    JsonNode nodes = member.path("nodes");
    JsonNode channels = member.path("channels");
    if (!nodes.isArray() || !channels.isArray() || nodes.size() != channels.size()) {
      throw invalidFormat("成员 nodes/channels 必须是等长数组");
    }

    int timelineSize = root.path("timestamps").size();
    List<Integer> selectedIndices = collectSelectedIndices(member, timelineSize);
    int expectedTransitions = Math.max(0, selectedIndices.size() - 1);
    if (summary.getTotalTransitions() == null
        || summary.getTotalTransitions().intValue() != expectedTransitions) {
      throw invalidFormat("成员 totalTransitions 与 changedPointIndices 不一致");
    }

    com.fasterxml.jackson.databind.node.ObjectNode normalized = objectMapper.createObjectNode();
    normalized.put("version", "2.3");
    normalized.put("fileId", root.path("fileId").asText() + ":" + summary.getLogicId());
    normalized.put("logicId", summary.getLogicId());
    copyField(root, normalized, "startTime");
    copyField(root, normalized, "endTime");
    copyField(root, normalized, "timeReference");
    copyField(root, normalized, "recordStartTime");
    normalized.put("totalTransitions", expectedTransitions);
    copyField(member, normalized, "experimentPassed");
    copyField(member, normalized, "experimentResult");
    copyField(member, normalized, "settings");
    normalized.set("nodes", nodes.deepCopy());
    normalized.set("timestamps", selectArray(root.path("timestamps"), selectedIndices));
    normalized.set("sampleIndices", selectArray(root.path("sampleIndices"), selectedIndices));

    com.fasterxml.jackson.databind.node.ArrayNode normalizedChannels = objectMapper.createArrayNode();
    Set<String> nodeIds = new HashSet<>();
    for (int channelIndex = 0; channelIndex < channels.size(); channelIndex++) {
      JsonNode node = nodes.get(channelIndex);
      JsonNode channel = channels.get(channelIndex);
      String nodeId = nullableText(node, "id");
      if (!node.isObject() || nodeId == null || nodeId.trim().isEmpty() || !nodeIds.add(nodeId)) {
        throw invalidFormat("成员 nodes 包含无效或重复的节点 ID");
      }
      if (!channel.isObject()
          || !channel.path("nodeIndex").isIntegralNumber()
          || !channel.path("nodeIndex").canConvertToInt()
          || channel.path("nodeIndex").asInt() != channelIndex
          || !nodeId.equals(nullableText(channel, "id"))) {
        throw invalidFormat("成员 channels 与 nodes 的数组位置不一致");
      }
      JsonNode values = channel.path("values");
      if (!values.isArray() || values.size() != timelineSize) {
        throw invalidFormat("成员通道 values 必须与根级时间轴等长");
      }
      for (JsonNode value : values) {
        if (!value.isIntegralNumber()) {
          throw invalidFormat("成员通道 values 只能包含整数");
        }
      }
      com.fasterxml.jackson.databind.node.ObjectNode normalizedChannel = channel.deepCopy();
      normalizedChannel.set("values", selectArray(values, selectedIndices));
      if (channel.has("elapsed")) {
        JsonNode elapsed = channel.path("elapsed");
        if (!elapsed.isArray() || elapsed.size() != timelineSize) {
          throw invalidFormat("定时器 elapsed 必须与根级时间轴等长");
        }
        for (JsonNode value : elapsed) {
          if (!value.isNumber() || value.asDouble() < 0) {
            throw invalidFormat("定时器 elapsed 只能包含非负数值");
          }
        }
        normalizedChannel.set("elapsed", selectArray(elapsed, selectedIndices));
      }
      normalizedChannels.add(normalizedChannel);
    }
    normalized.set("channels", normalizedChannels);

    try {
      return objectMapper.writeValueAsString(normalized);
    } catch (Exception e) {
      throw invalidFormat("成员断面序列化失败");
    }
  }

  private List<Integer> collectSelectedIndices(JsonNode member, int timelineSize) {
    JsonNode changedPointIndices = member.path("changedPointIndices");
    if (!changedPointIndices.isArray()) {
      throw invalidFormat("成员 changedPointIndices 必须是数组");
    }
    Set<Integer> unique = new LinkedHashSet<>();
    unique.add(0);
    for (JsonNode item : changedPointIndices) {
      if (!item.isIntegralNumber() || !item.canConvertToInt()) {
        throw invalidFormat("changedPointIndices 包含非整数");
      }
      int index = item.asInt();
      if (index < 0 || index >= timelineSize) {
        throw invalidFormat("changedPointIndices 越界: " + index);
      }
      unique.add(index);
    }
    List<Integer> result = new ArrayList<>(unique);
    Collections.sort(result);
    return result;
  }

  private com.fasterxml.jackson.databind.node.ArrayNode selectArray(
      JsonNode source, List<Integer> indices) {
    com.fasterxml.jackson.databind.node.ArrayNode result = objectMapper.createArrayNode();
    for (Integer index : indices) {
      result.add(source.get(index));
    }
    return result;
  }

  private void copyField(
      JsonNode source,
      com.fasterxml.jackson.databind.node.ObjectNode target,
      String fieldName) {
    if (source.has(fieldName) && !source.get(fieldName).isNull()) {
      target.set(fieldName, source.get(fieldName).deepCopy());
    }
  }

  private String nullableText(JsonNode node, String fieldName) {
    return node.has(fieldName) && !node.get(fieldName).isNull()
        ? node.get(fieldName).asText() : null;
  }

  private Boolean nullableBoolean(JsonNode node, String fieldName) {
    return node.has(fieldName) && node.get(fieldName).isBoolean()
        ? node.get(fieldName).asBoolean() : null;
  }

  private ResponseStatusException invalidFormat(String message) {
    return new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY, message);
  }
}
