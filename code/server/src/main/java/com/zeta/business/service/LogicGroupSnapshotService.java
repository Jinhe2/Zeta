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
import java.time.Instant;
import java.util.HashSet;
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
    return parser().listMembers(snapshot.getSnapshotJson(), memberIds(snapshot.getGroupId()));
  }

  public MemberDetailResponse getMember(Long userId, Long snapshotId, Long logicDiagramId) {
    LogicGroupSnapshot snapshot = get(userId, snapshotId);
    return parser().getMember(snapshotId, snapshot.getGroupId(), snapshot.getSnapshotJson(),
        memberIds(snapshot.getGroupId()), logicDiagramId);
  }

  private Set<Long> memberIds(Long groupId) {
    Set<Long> ids = new HashSet<>();
    memberRepository.findByGroupIdOrderBySortOrderAscIdAsc(groupId)
        .forEach(member -> ids.add(member.getLogicDiagramId()));
    return ids;
  }

  private LogicGroupSnapshotParser parser() {
    return new LogicGroupSnapshotParser(objectMapper, logicSnapshotService);
  }
}
