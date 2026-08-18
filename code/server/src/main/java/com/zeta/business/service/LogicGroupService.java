package com.zeta.business.service;

import com.zeta.business.entities.logicgroup.LogicGroup;
import com.zeta.business.entities.logicgroup.LogicGroupMember;
import com.zeta.business.entities.logicgroup.LogicGroupMemberRepository;
import com.zeta.business.entities.logicgroup.LogicGroupRepository;
import com.zeta.business.entities.logicgroup.dto.LogicGroupDtos.*;
import com.zeta.screen.ieddevice.Device;
import com.zeta.screen.ieddevice.DeviceRepository;
import com.zeta.screen.logicdiagram.ProtectionLogic;
import com.zeta.screen.logicdiagram.ProtectionLogicRepository;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class LogicGroupService {
  private final LogicGroupRepository groupRepository;
  private final LogicGroupMemberRepository memberRepository;
  private final ProtectionLogicRepository protectionLogicRepository;
  private final DeviceRepository deviceRepository;

  public LogicGroupService(
      LogicGroupRepository groupRepository,
      LogicGroupMemberRepository memberRepository,
      ProtectionLogicRepository protectionLogicRepository,
      DeviceRepository deviceRepository) {
    this.groupRepository = groupRepository;
    this.memberRepository = memberRepository;
    this.protectionLogicRepository = protectionLogicRepository;
    this.deviceRepository = deviceRepository;
  }

  @Transactional(value = "businessTransactionManager", readOnly = true)
  public List<LogicGroupResponse> listByDevice(Long deviceId) {
    return groupRepository.findByIedDeviceIdOrderBySortOrderAscIdAsc(deviceId).stream()
        .map(this::toResponse)
        .collect(Collectors.toList());
  }

  @Transactional(value = "businessTransactionManager", readOnly = true)
  public LogicGroupDetailResponse get(Long groupId) {
    LogicGroup group = requireGroup(groupId);
    Device device = requireDevice(group.getIedDeviceId());
    return toDetail(group, device);
  }

  @Transactional("businessTransactionManager")
  public LogicGroupDetailResponse create(Long deviceId, CreateLogicGroupRequest request) {
    requireDevice(deviceId);
    LogicGroup group = new LogicGroup();
    group.setIedDeviceId(deviceId);
    group.setName(request.getName());
    group.setSortOrder(request.getSortOrder() == null ? 0 : request.getSortOrder());
    groupRepository.save(group);
    groupRepository.flush();
    replaceMembers(group.getId(), request.getMembers(), deviceId);
    memberRepository.flush();
    return get(group.getId());
  }

  @Transactional("businessTransactionManager")
  public LogicGroupDetailResponse update(Long groupId, UpdateLogicGroupRequest request) {
    LogicGroup group = requireGroup(groupId);
    group.setName(request.getName());
    group.setSortOrder(request.getSortOrder() == null ? 0 : request.getSortOrder());
    groupRepository.save(group);
    replaceMembers(groupId, request.getMembers(), group.getIedDeviceId());
    memberRepository.flush();
    return get(groupId);
  }

  @Transactional("businessTransactionManager")
  public void delete(Long groupId) {
    requireGroup(groupId);
    memberRepository.deleteByGroupId(groupId);
    groupRepository.deleteById(groupId);
    groupRepository.flush();
  }

  /** 学员端只读：装置下的组合列表。 */
  @Transactional(value = "businessTransactionManager", readOnly = true)
  public List<LogicGroupResponse> listKnowledgeByDevice(Long deviceId) {
    return listByDevice(deviceId);
  }

  /** 学员端只读：组合详情。 */
  @Transactional(value = "businessTransactionManager", readOnly = true)
  public LogicGroupDetailResponse getDetail(Long groupId) {
    return get(groupId);
  }

  /** 实验用：解析组合对应的装置名与按序排列的 logic_ids。 */
  @Transactional(value = "businessTransactionManager", readOnly = true)
  public ExperimentTarget resolveForExperiment(Long groupId) {
    LogicGroup group = requireGroup(groupId);
    Device device = requireDevice(group.getIedDeviceId());
    List<LogicGroupMember> members = memberRepository.findByGroupIdOrderBySortOrderAscIdAsc(groupId);
    List<String> logicIds = new ArrayList<>();
    for (LogicGroupMember member : members) {
      ProtectionLogic logic =
          protectionLogicRepository
              .findById(member.getLogicDiagramId())
              .orElseThrow(
                  () ->
                      new ResponseStatusException(
                          HttpStatus.NOT_FOUND,
                          "组合成员逻辑框图不存在: " + member.getLogicDiagramId()));
      logicIds.add(logic.getLogicId());
    }
    if (logicIds.isEmpty() || logicIds.size() > 16) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "组合逻辑成员数量需在 1~16 之间");
    }
    return new ExperimentTarget(device.getIedName(), logicIds);
  }

  private void replaceMembers(Long groupId, List<MemberRequest> members, Long deviceId) {
    memberRepository.deleteByGroupId(groupId);
    memberRepository.flush();
    if (members == null || members.isEmpty()) {
      throw badRequest("组合逻辑至少需要一个基础逻辑");
    }
    Set<Long> seen = new HashSet<>();
    int index = 0;
    for (MemberRequest member : members) {
      Long logicId = member.getLogicDiagramId();
      if (logicId == null) {
        throw badRequest("成员逻辑框图不能为空");
      }
      if (!seen.add(logicId)) {
        throw badRequest("成员逻辑框图重复: " + logicId);
      }
      ProtectionLogic logic =
          protectionLogicRepository
              .findById(logicId)
              .orElseThrow(() -> badRequest("逻辑框图不存在: " + logicId));
      if (logic.getDevice() == null || !deviceId.equals(logic.getDevice().getId())) {
        throw badRequest("逻辑框图不属于当前装置: " + logicId);
      }
      LogicGroupMember entity = new LogicGroupMember();
      entity.setGroupId(groupId);
      entity.setLogicDiagramId(logicId);
      entity.setSortOrder(member.getSortOrder() == null ? index : member.getSortOrder());
      memberRepository.save(entity);
      index++;
    }
  }

  private LogicGroup requireGroup(Long groupId) {
    return groupRepository
        .findById(groupId)
        .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "组合逻辑不存在"));
  }

  private Device requireDevice(Long deviceId) {
    return deviceRepository
        .findById(deviceId)
        .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "装置不存在"));
  }

  private LogicGroupResponse toResponse(LogicGroup group) {
    int count = (int) memberRepository.countByGroupId(group.getId());
    return new LogicGroupResponse(
        group.getId(), group.getIedDeviceId(), group.getName(), group.getSortOrder(), count);
  }

  private LogicGroupDetailResponse toDetail(LogicGroup group, Device device) {
    List<LogicGroupMember> members =
        memberRepository.findByGroupIdOrderBySortOrderAscIdAsc(group.getId());
    List<MemberResponse> memberResponses = new ArrayList<>();
    for (LogicGroupMember member : members) {
      ProtectionLogic logic =
          protectionLogicRepository.findById(member.getLogicDiagramId()).orElse(null);
      memberResponses.add(
          new MemberResponse(
              member.getLogicDiagramId(),
              logic == null ? null : logic.getLogicId(),
              logic == null ? null : logic.getLogicName(),
              member.getSortOrder()));
    }
    return new LogicGroupDetailResponse(
        group.getId(),
        group.getIedDeviceId(),
        device.getIedName(),
        group.getName(),
        group.getSortOrder(),
        memberResponses);
  }

  private ResponseStatusException badRequest(String message) {
    return new ResponseStatusException(HttpStatus.BAD_REQUEST, message);
  }

  @Getter
  @AllArgsConstructor
  public static class ExperimentTarget {
    private String iedName;
    private List<String> logicIds;
  }
}
