package com.zeta.business.service;

import com.zeta.business.entities.learningpath.*;
import com.zeta.business.entities.user.User;
import com.zeta.business.entities.user.UserRepository;
import com.zeta.business.entities.user.UserRole;
import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class LearningPathService {
  private final LearningModuleRepository modules;
  private final LearningModulePrerequisiteRepository prerequisites;
  private final StudentLearningProgressRepository progress;
  private final LearningProgressEventRepository events;
  private final UserRepository users;

  public LearningPathService(LearningModuleRepository modules,
      LearningModulePrerequisiteRepository prerequisites,
      StudentLearningProgressRepository progress,
      LearningProgressEventRepository events, UserRepository users) {
    this.modules = modules;
    this.prerequisites = prerequisites;
    this.progress = progress;
    this.events = events;
    this.users = users;
  }

  @Transactional("businessTransactionManager")
  public Map<String, Object> studentPath(Long userId) {
    users.findByIdForActivityUpdate(userId)
        .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "学员不存在"));
    return buildStudentPath(userId, Instant.now());
  }

  @Transactional("businessTransactionManager")
  public Map<String, Object> visit(Long userId, String rawCode) {
    String code = normalizeCode(rawCode);
    users.findByIdForActivityUpdate(userId)
        .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "学员不存在"));
    buildStudentPath(userId, Instant.now());
    StudentLearningProgress item = progress.findByUserIdAndModuleCode(userId, code)
        .orElseThrow(() -> new ResponseStatusException(HttpStatus.FORBIDDEN, "该学习模块尚未解锁"));
    Instant now = Instant.now();
    if (item.getStatus() == StudentLearningStatus.AVAILABLE) {
      item.setStatus(StudentLearningStatus.IN_PROGRESS);
      item.setStartedAt(now);
      record(userId, code, "STARTED", null, "学员首次进入模块", now);
    }
    item.setLastActivityAt(now);
    item.setVisitCount(item.getVisitCount() + 1);
    progress.save(item);
    return buildStudentPath(userId, now);
  }

  @Transactional("businessTransactionManager")
  public Map<String, Object> complete(Long userId, String rawCode) {
    String code = normalizeCode(rawCode);
    users.findByIdForActivityUpdate(userId)
        .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "学员不存在"));
    buildStudentPath(userId, Instant.now());
    StudentLearningProgress item = progress.findByUserIdAndModuleCode(userId, code)
        .orElseThrow(() -> new ResponseStatusException(HttpStatus.FORBIDDEN, "该学习模块尚未解锁"));
    Instant now = Instant.now();
    if (item.getStatus() == StudentLearningStatus.AVAILABLE) {
      throw new ResponseStatusException(HttpStatus.CONFLICT, "请先进入该模块开始学习");
    }
    if (item.getStatus() != StudentLearningStatus.COMPLETED) {
      item.setStatus(StudentLearningStatus.COMPLETED);
      if (item.getStartedAt() == null) item.setStartedAt(now);
      item.setCompletedAt(now);
      item.setLastActivityAt(now);
      progress.save(item);
      record(userId, code, "COMPLETED", null, "学员确认完成本阶段学习", now);
    }
    return buildStudentPath(userId, now);
  }

  @Transactional(value = "businessTransactionManager", readOnly = true)
  public List<Map<String, Object>> config() {
    Map<String, List<String>> dependencyMap = dependencyMap();
    List<Map<String, Object>> result = new ArrayList<>();
    for (LearningModule module : modules.findAllByOrderBySortOrderAscCodeAsc()) {
      result.add(moduleConfigRow(module, dependencyMap.getOrDefault(module.getCode(), Collections.emptyList())));
    }
    return result;
  }

  @Transactional("businessTransactionManager")
  public List<Map<String, Object>> saveConfig(List<Map<String, Object>> rows) {
    if (rows == null || rows.isEmpty()) throw bad("学习路径不能为空");
    Map<String, LearningModule> existing = modules.findAll().stream()
        .collect(Collectors.toMap(LearningModule::getCode, module -> module));
    if (rows.size() != existing.size()) throw bad("必须提交全部学习模块配置");
    Map<String, List<String>> nextDependencies = new LinkedHashMap<>();
    Set<Integer> orders = new HashSet<>();
    for (Map<String, Object> row : rows) {
      String code = normalizeCode(String.valueOf(row.get("code")));
      LearningModule module = existing.get(code);
      if (module == null) throw bad("未知学习模块：" + code);
      int sortOrder = number(row.get("sortOrder"), "排序");
      if (!orders.add(sortOrder)) throw bad("模块排序不能重复");
      module.setSortOrder(sortOrder);
      module.setEnabled(Boolean.TRUE.equals(row.get("enabled")));
      modules.save(module);
      List<String> deps = strings(row.get("prerequisites"));
      for (String dependency : deps) {
        if (!existing.containsKey(dependency)) throw bad("未知前置模块：" + dependency);
        if (code.equals(dependency)) throw bad("模块不能依赖自身");
      }
      nextDependencies.put(code, new ArrayList<>(new LinkedHashSet<>(deps)));
    }
    validateAcyclic(nextDependencies);
    for (Map.Entry<String, List<String>> entry : nextDependencies.entrySet()) {
      if (!existing.get(entry.getKey()).isEnabled()) continue;
      for (String dependency : entry.getValue()) {
        if (!existing.get(dependency).isEnabled()) throw bad("启用模块不能依赖已停用模块");
      }
    }
    prerequisites.deleteAllInBatch();
    for (Map.Entry<String, List<String>> entry : nextDependencies.entrySet()) {
      for (String dependency : entry.getValue()) {
        LearningModulePrerequisite item = new LearningModulePrerequisite();
        item.setModuleCode(entry.getKey());
        item.setPrerequisiteCode(dependency);
        prerequisites.save(item);
      }
    }
    return config();
  }

  @Transactional("businessTransactionManager")
  public List<Map<String, Object>> monitor() {
    List<Map<String, Object>> result = new ArrayList<>();
    for (User user : users.findAllByRoleOrderByCreatedAtAsc(UserRole.STUDENT)) {
      Map<String, Object> path = buildStudentPath(user.getId(), Instant.now());
      @SuppressWarnings("unchecked")
      List<Map<String, Object>> items = (List<Map<String, Object>>) path.get("modules");
      int completed = 0;
      int unlocked = 0;
      String currentStep = "尚未开始";
      for (Map<String, Object> item : items) {
        String status = String.valueOf(item.get("status"));
        if (!"LOCKED".equals(status)) unlocked++;
        if ("COMPLETED".equals(status)) completed++;
        if ("IN_PROGRESS".equals(status)) currentStep = String.valueOf(item.get("name"));
        else if ("尚未开始".equals(currentStep) && "AVAILABLE".equals(status)) {
          currentStep = String.valueOf(item.get("name"));
        }
      }
      Map<String, Object> row = new LinkedHashMap<>();
      row.put("userId", user.getId());
      row.put("studentNo", user.getStudentNo());
      row.put("studentName", user.getDisplayName());
      row.put("completedCount", completed);
      row.put("unlockedCount", unlocked);
      row.put("totalCount", items.size());
      row.put("currentStep", currentStep);
      row.put("modules", items);
      result.add(row);
    }
    return result;
  }

  private Map<String, Object> buildStudentPath(Long userId, Instant now) {
    List<LearningModule> enabled = modules.findByEnabledTrueOrderBySortOrderAscCodeAsc();
    Map<String, List<String>> dependencyMap = dependencyMap();
    Map<String, StudentLearningProgress> byCode = progress.findByUserId(userId).stream()
        .collect(Collectors.toMap(StudentLearningProgress::getModuleCode, item -> item));
    boolean changed;
    do {
      changed = false;
      for (LearningModule module : enabled) {
        if (byCode.containsKey(module.getCode())) continue;
        List<String> deps = dependencyMap.getOrDefault(module.getCode(), Collections.emptyList());
        boolean satisfied = deps.stream().allMatch(code -> {
          StudentLearningProgress prerequisite = byCode.get(code);
          return prerequisite != null && prerequisite.getStatus() == StudentLearningStatus.COMPLETED;
        });
        if (satisfied) {
          StudentLearningProgress item = new StudentLearningProgress();
          item.setUserId(userId);
          item.setModuleCode(module.getCode());
          item.setStatus(StudentLearningStatus.AVAILABLE);
          item.setUnlockedAt(now);
          item.setVisitCount(0);
          progress.save(item);
          byCode.put(module.getCode(), item);
          record(userId, module.getCode(), "UNLOCKED", null, "满足前置模块条件", now);
          changed = true;
        }
      }
    } while (changed);

    List<Map<String, Object>> rows = new ArrayList<>();
    for (LearningModule module : enabled) {
      StudentLearningProgress item = byCode.get(module.getCode());
      List<String> deps = dependencyMap.getOrDefault(module.getCode(), Collections.emptyList());
      Map<String, Object> row = moduleConfigRow(module, deps);
      row.put("status", item == null ? "LOCKED" : item.getStatus().name());
      row.put("unlockedAt", item == null ? null : item.getUnlockedAt());
      row.put("startedAt", item == null ? null : item.getStartedAt());
      row.put("completedAt", item == null ? null : item.getCompletedAt());
      row.put("lastActivityAt", item == null ? null : item.getLastActivityAt());
      row.put("visitCount", item == null ? 0 : item.getVisitCount());
      if (item == null) {
        List<String> reasons = deps.stream().filter(code -> {
          StudentLearningProgress dependency = byCode.get(code);
          return dependency == null || dependency.getStatus() != StudentLearningStatus.COMPLETED;
        }).map(code -> modules.findById(code).map(LearningModule::getName).orElse(code))
          .collect(Collectors.toList());
        row.put("lockedReasons", reasons);
      } else row.put("lockedReasons", Collections.emptyList());
      rows.add(row);
    }
    Map<String, Object> result = new LinkedHashMap<>();
    result.put("modules", rows);
    return result;
  }

  private Map<String, Object> moduleConfigRow(LearningModule module, List<String> deps) {
    Map<String, Object> row = new LinkedHashMap<>();
    row.put("code", module.getCode());
    row.put("name", module.getName());
    row.put("description", module.getDescription());
    row.put("sortOrder", module.getSortOrder());
    row.put("enabled", module.isEnabled());
    row.put("completionMode", module.getCompletionMode());
    row.put("prerequisites", deps);
    return row;
  }

  private Map<String, List<String>> dependencyMap() {
    Map<String, List<String>> result = new LinkedHashMap<>();
    for (LearningModulePrerequisite item : prerequisites.findAllByOrderByIdAsc()) {
      result.computeIfAbsent(item.getModuleCode(), ignored -> new ArrayList<>())
          .add(item.getPrerequisiteCode());
    }
    return result;
  }

  private void record(Long userId, String code, String type, Long operatorId, String note, Instant now) {
    LearningProgressEvent event = new LearningProgressEvent();
    event.setUserId(userId);
    event.setModuleCode(code);
    event.setEventType(type);
    event.setOperatorUserId(operatorId);
    event.setOccurredAt(now);
    event.setNote(note);
    events.save(event);
  }

  private static void validateAcyclic(Map<String, List<String>> graph) {
    Set<String> visiting = new HashSet<>();
    Set<String> visited = new HashSet<>();
    for (String code : graph.keySet()) visitGraph(code, graph, visiting, visited);
  }

  private static void visitGraph(String code, Map<String, List<String>> graph,
      Set<String> visiting, Set<String> visited) {
    if (visited.contains(code)) return;
    if (!visiting.add(code)) throw bad("学习路径不能形成循环依赖");
    for (String dependency : graph.getOrDefault(code, Collections.emptyList())) {
      visitGraph(dependency, graph, visiting, visited);
    }
    visiting.remove(code);
    visited.add(code);
  }

  private static List<String> strings(Object value) {
    if (!(value instanceof Collection)) return Collections.emptyList();
    List<String> result = new ArrayList<>();
    for (Object item : (Collection<?>) value) result.add(normalizeCode(String.valueOf(item)));
    return result;
  }

  private static int number(Object value, String label) {
    try { return Integer.parseInt(String.valueOf(value)); }
    catch (RuntimeException ex) { throw bad(label + "必须是整数"); }
  }

  private static String normalizeCode(String value) {
    String code = value == null ? "" : value.trim().toUpperCase(Locale.ROOT);
    if (!code.matches("[A-Z][A-Z0-9_]{1,31}")) throw bad("模块编码无效");
    return code;
  }

  private static ResponseStatusException bad(String message) {
    return new ResponseStatusException(HttpStatus.BAD_REQUEST, message);
  }
}
