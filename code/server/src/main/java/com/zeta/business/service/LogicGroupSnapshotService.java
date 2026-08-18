package com.zeta.business.service;

import com.zeta.business.entities.logicgroup.LogicGroup;
import com.zeta.business.entities.logicgroup.LogicGroupRepository;
import com.zeta.business.entities.logicgroup.LogicGroupSnapshot;
import com.zeta.business.entities.logicgroup.LogicGroupSnapshotRepository;
import java.time.Instant;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class LogicGroupSnapshotService {

  private final LogicGroupSnapshotRepository snapshotRepository;
  private final LogicGroupRepository groupRepository;

  public LogicGroupSnapshotService(
      LogicGroupSnapshotRepository snapshotRepository, LogicGroupRepository groupRepository) {
    this.snapshotRepository = snapshotRepository;
    this.groupRepository = groupRepository;
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
}
