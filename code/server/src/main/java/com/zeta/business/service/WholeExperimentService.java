package com.zeta.business.service;

import com.zeta.business.entities.wholeexperiment.*;
import com.zeta.business.entities.wholeexperiment.WholeExperimentDtos.*;
import com.zeta.screen.logicdiagram.ProtectionLogic;
import com.zeta.screen.logicdiagram.ProtectionLogicRepository;
import com.zeta.screen.ieddevice.DeviceRepository;
import java.util.*;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
@RequiredArgsConstructor
@Transactional(value = "businessTransactionManager", readOnly = true)
public class WholeExperimentService {
  private final WholeExperimentRepository repository;
  private final WholeExperimentMemberRepository members;
  private final ProtectionLogicRepository logics;
  private final DeviceRepository devices;
  private final LogicLearningConfigService configs;

  @Transactional("businessTransactionManager")
  public Detail create(Long userId, CreateRequest request) {
    List<Member> ordered = validate(request.getDeviceId(), request.getLogicDiagramIds());
    String signature = ordered.stream().map(m -> String.valueOf(m.getLogicDiagramId()))
        .collect(Collectors.joining(","));
    repository.insertIfAbsent(userId, request.getDeviceId(), signature);
    WholeExperiment experiment = repository.lockBySignature(userId, request.getDeviceId(), signature);
    if (members.findByExperimentIdOrderBySequenceNoAsc(experiment.getId()).isEmpty()) {
      for (Member item : ordered) {
        WholeExperimentMember member = new WholeExperimentMember();
        member.setExperimentId(experiment.getId());
        member.setLogicDiagramId(item.getLogicDiagramId());
        member.setSequenceNo(item.getSortOrder());
        member.setCode(item.getCode());
        member.setTitle(item.getTitle());
        members.save(member);
      }
      members.flush();
    }
    return detail(userId, experiment.getId());
  }

  public WholeExperiment require(Long userId, Long id) {
    return repository.findByIdAndUserId(id, userId)
        .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "整组实验不存在"));
  }

  public List<Detail> recent(Long userId, Long deviceId) {
    return repository.findTop5ByUserIdAndDeviceIdAndLastStartedAtIsNotNullOrderByLastStartedAtDescIdDesc(
        userId, deviceId).stream().map(e -> detail(userId, e.getId())).collect(Collectors.toList());
  }

  public Detail detail(Long userId, Long id) {
    WholeExperiment experiment = require(userId, id);
    List<Member> saved = savedMembers(id);
    String reason = null;
    List<Member> current = saved;
    try {
      current = validatedMembers(userId, id);
    } catch (ResponseStatusException ex) {
      reason = ex.getReason();
    }
    String iedName = devices.findById(experiment.getDeviceId()).map(d -> d.getIedName()).orElse(null);
    String name = current.stream().map(Member::getTitle).collect(Collectors.joining(" → "));
    return new Detail(id, experiment.getDeviceId(), iedName, name, current,
        experiment.getLastStartedAt(), reason == null, reason);
  }

  public List<Member> savedMembers(Long id) {
    return members.findByExperimentIdOrderBySequenceNoAsc(id).stream()
        .map(m -> new Member(m.getLogicDiagramId(), m.getCode(), m.getTitle(), m.getSequenceNo()))
        .collect(Collectors.toList());
  }

  public List<Member> validatedMembers(Long userId, Long id) {
    WholeExperiment experiment = require(userId, id);
    List<Member> saved = savedMembers(id);
    List<Member> current = validate(experiment.getDeviceId(), ids(saved));
    if (!ids(saved).equals(ids(current))) {
      throw badRequest("基础逻辑序列已变化，请重新选择整组实验");
    }
    return current;
  }

  public List<Member> validate(Long deviceId, List<Long> ids) {
    if (deviceId == null || ids == null || ids.size() < 2 || ids.size() > 3
        || ids.contains(null) || new HashSet<>(ids).size() != ids.size()) {
      throw badRequest("请选择同一装置下两个或三个不重复的基础逻辑");
    }
    Map<Long, Integer> sequences = configs.getWholeExperimentSequences(ids);
    List<Member> result = new ArrayList<>();
    Set<String> codes = new HashSet<>();
    for (Long id : ids) {
      ProtectionLogic logic = logics.findById(id)
          .orElseThrow(() -> badRequest("基础逻辑不存在，请重新选择"));
      if (logic.getDevice() == null || !deviceId.equals(logic.getDevice().getId())) {
        throw badRequest("基础逻辑不属于当前装置，请重新选择");
      }
      if (logic.getLogicId() == null || logic.getLogicId().trim().isEmpty()
          || !codes.add(logic.getLogicId())) {
        throw badRequest("基础逻辑监测编码为空或重复，请联系管理员");
      }
      result.add(new Member(id, logic.getLogicId(), logic.getTitle(), sequences.getOrDefault(id, 1)));
    }
    result.sort(Comparator.comparingInt(Member::getSortOrder));
    for (int i = 0; i < result.size(); i++) {
      if (result.get(i).getSortOrder() != i + 1) {
        throw badRequest("整组实验必须从序列 1 开始连续选择，每个序列只能选择一个逻辑");
      }
    }
    return result;
  }

  public static List<Long> ids(List<Member> items) {
    return items.stream().map(Member::getLogicDiagramId).collect(Collectors.toList());
  }

  private ResponseStatusException badRequest(String message) {
    return new ResponseStatusException(HttpStatus.BAD_REQUEST, message);
  }
}
