package com.zeta.business.service;

import com.zeta.business.entities.binding.CabinetBinding;
import com.zeta.business.entities.binding.CabinetBindingRepository;
import com.zeta.business.entities.presence.StudentActivitySession;
import com.zeta.business.entities.presence.StudentActivitySessionRepository;
import com.zeta.business.entities.presence.StudentActivityStatus;
import com.zeta.business.entities.user.User;
import com.zeta.business.entities.user.UserRepository;
import com.zeta.screen.cabinet.CabinetRepository;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.HashMap;
import java.time.ZoneId;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class StudentActivityService {
  private final StudentActivitySessionRepository sessions;
  private final UserRepository users;
  private final CabinetBindingRepository bindings;
  private final CabinetRepository cabinets;

  @Value("${zeta.student-activity.idle-window-seconds:300}")
  private long idleWindowSeconds;
  @Value("${zeta.student-activity.write-throttle-seconds:15}")
  private long writeThrottleSeconds;

  public StudentActivityService(StudentActivitySessionRepository sessions, UserRepository users,
      CabinetBindingRepository bindings, CabinetRepository cabinets) {
    this.sessions = sessions;
    this.users = users;
    this.bindings = bindings;
    this.cabinets = cabinets;
  }

  @Transactional("businessTransactionManager")
  public void registerActivity(Long userId, String rawBindId, Instant now) {
    String bindId = normalize(rawBindId);
    if (bindId == null) return;
    CabinetBinding binding = bindings.findByBindIdForActivityUpdate(bindId).orElse(null);
    if (binding == null) return;
    User user = users.findByIdForActivityUpdate(userId).orElse(null);
    if (user == null) return;

    // 同一台绑定设备只保留一个学员的有效会话；正常换账号和异常退出都能及时收口。
    for (StudentActivitySession session :
        sessions.findByDeviceBindIdAndStatus(bindId, StudentActivityStatus.ACTIVE)) {
      if (!session.getUserId().equals(userId)) {
        Instant end = now.isBefore(session.getExpiresAt()) ? now : session.getExpiresAt();
        close(session, end, now.isBefore(session.getExpiresAt())
            ? StudentActivityStatus.REPLACED : StudentActivityStatus.EXPIRED);
      }
    }

    StudentActivitySession current = null;
    for (StudentActivitySession session : sessions.findByUserIdAndStatus(userId, StudentActivityStatus.ACTIVE)) {
      if (!now.isBefore(session.getExpiresAt())) {
        close(session, session.getExpiresAt(), StudentActivityStatus.EXPIRED);
      } else if (!session.getDeviceBindId().equals(bindId)) {
        close(session, now, StudentActivityStatus.REPLACED);
      } else if (!session.getCabinetId().equals(binding.getScreenCabinetId())) {
        close(session, now, StudentActivityStatus.CABINET_CHANGED);
      } else {
        current = session;
      }
    }
    if (current == null) {
      current = new StudentActivitySession();
      current.setUserId(userId);
      current.setCabinetId(binding.getScreenCabinetId());
      current.setCabinetNameSnapshot(cabinets.findById(binding.getScreenCabinetId())
          .map(cabinet -> cabinet.getName()).orElse(null));
      current.setDeviceBindId(bindId);
      current.setBindLabelSnapshot(binding.getBindLabel());
      current.setStartedAt(now);
      current.setLastActivityAt(now);
      current.setExpiresAt(now.plusSeconds(idleWindowSeconds));
      current.setDurationSeconds(idleWindowSeconds);
      current.setStatus(StudentActivityStatus.ACTIVE);
      sessions.save(current);
      return;
    }
    if (Duration.between(current.getLastActivityAt(), now).getSeconds() < writeThrottleSeconds) return;
    current.setLastActivityAt(now);
    current.setExpiresAt(now.plusSeconds(idleWindowSeconds));
    current.setDurationSeconds(seconds(current.getStartedAt(), current.getExpiresAt()));
    sessions.save(current);
  }

  @Transactional("businessTransactionManager")
  public void endActivity(Long userId, String rawBindId, Instant now) {
    String bindId = normalize(rawBindId);
    users.findByIdForActivityUpdate(userId);
    for (StudentActivitySession session : sessions.findByUserIdAndStatus(userId, StudentActivityStatus.ACTIVE)) {
      if (bindId == null || bindId.equals(session.getDeviceBindId())) {
        close(session, now.isBefore(session.getExpiresAt()) ? now : session.getExpiresAt(),
            StudentActivityStatus.LOGOUT);
      }
    }
  }

  @Scheduled(fixedDelayString = "${zeta.student-activity.cleanup-interval-ms:60000}")
  @Transactional("businessTransactionManager")
  public void expireStaleSessions() {
    Instant now = Instant.now();
    for (StudentActivitySession session :
        sessions.findByStatusAndExpiresAtBefore(StudentActivityStatus.ACTIVE, now)) {
      close(session, session.getExpiresAt(), StudentActivityStatus.EXPIRED);
    }
  }

  @Transactional(value = "businessTransactionManager", readOnly = true)
  public List<Map<String, Object>> sessionReport(Instant from, Instant to, Long userId, Long cabinetId) {
    List<Map<String, Object>> result = new ArrayList<>();
    for (StudentActivitySession session : sessions.findOverlapping(from, to, userId, cabinetId)) {
      Instant start = session.getStartedAt().isAfter(from) ? session.getStartedAt() : from;
      Instant effectiveEnd = effectiveEnd(session, Instant.now());
      Instant end = effectiveEnd.isBefore(to) ? effectiveEnd : to;
      Map<String, Object> row = new LinkedHashMap<>();
      row.put("id", session.getId());
      row.put("userId", session.getUserId());
      users.findById(session.getUserId()).ifPresent(user -> {
        row.put("studentNo", user.getStudentNo());
        row.put("studentName", user.getDisplayName());
      });
      row.put("cabinetId", session.getCabinetId());
      row.put("cabinetName", session.getCabinetNameSnapshot());
      row.put("deviceBindId", session.getDeviceBindId());
      row.put("bindLabel", session.getBindLabelSnapshot());
      row.put("startedAt", session.getStartedAt());
      row.put("lastActivityAt", session.getLastActivityAt());
      row.put("expiresAt", session.getExpiresAt());
      row.put("endedAt", session.getEndedAt());
      row.put("status", session.getStatus());
      row.put("durationSeconds", Math.max(0, seconds(start, end)));
      result.add(row);
    }
    return result;
  }

  @Transactional(value = "businessTransactionManager", readOnly = true)
  public List<Map<String, Object>> summaryReport(Instant from, Instant to, Long userId, Long cabinetId) {
    Map<String, Map<String, Object>> grouped = new LinkedHashMap<>();
    for (Map<String, Object> session : sessionReport(from, to, userId, cabinetId)) {
      String key = session.get("userId") + ":" + session.get("cabinetId");
      Map<String, Object> row = grouped.get(key);
      if (row == null) {
        row = new LinkedHashMap<>();
        row.put("userId", session.get("userId"));
        row.put("studentNo", session.get("studentNo"));
        row.put("studentName", session.get("studentName"));
        row.put("cabinetId", session.get("cabinetId"));
        row.put("cabinetName", session.get("cabinetName"));
        row.put("durationSeconds", 0L);
        row.put("sessionCount", 0);
        grouped.put(key, row);
      }
      row.put("durationSeconds", (Long) row.get("durationSeconds")
          + ((Number) session.get("durationSeconds")).longValue());
      row.put("sessionCount", (Integer) row.get("sessionCount") + 1);
    }
    return new ArrayList<>(grouped.values());
  }

  @Transactional(value = "businessTransactionManager", readOnly = true)
  public Map<String, Object> mySummary(Long userId, String rawBindId, Instant now) {
    String bindId = normalize(rawBindId);
    CabinetBinding binding = bindId == null ? null : bindings.findByBindId(bindId).orElse(null);
    Long cabinetId = binding == null ? null : binding.getScreenCabinetId();
    long totalSeconds = valueOrZero(sessions.sumDurationByUserId(userId));
    long cabinetSeconds = cabinetId == null
        ? 0 : valueOrZero(sessions.sumDurationByUserIdAndCabinetId(userId, cabinetId));
    StudentActivitySession current = null;
    for (StudentActivitySession session :
        sessions.findByUserIdAndStatus(userId, StudentActivityStatus.ACTIVE)) {
      long storedSeconds = session.getDurationSeconds();
      long effectiveSeconds = seconds(session.getStartedAt(), effectiveEnd(session, now));
      totalSeconds += effectiveSeconds - storedSeconds;
      if (cabinetId != null && cabinetId.equals(session.getCabinetId())) {
        cabinetSeconds += effectiveSeconds - storedSeconds;
        if (now.isBefore(session.getExpiresAt())
            && (bindId == null || bindId.equals(session.getDeviceBindId()))) {
          current = session;
        }
      }
    }
    Map<String, Object> result = new LinkedHashMap<>();
    result.put("measuredAt", now);
    result.put("online", current != null);
    result.put("currentSessionSeconds", current == null ? 0L : seconds(current.getStartedAt(), now));
    result.put("sessionStartedAt", current == null ? null : current.getStartedAt());
    result.put("currentCabinetId", cabinetId);
    result.put("currentCabinetName", cabinetId == null ? null
        : cabinets.findById(cabinetId).map(cabinet -> cabinet.getName()).orElse(null));
    result.put("currentCabinetTotalSeconds", Math.max(0, cabinetSeconds));
    result.put("totalSeconds", Math.max(0, totalSeconds));
    return result;
  }

  @Transactional(value = "businessTransactionManager", readOnly = true)
  public List<Map<String, Object>> cabinetOnlineReport(Instant now) {
    Instant todayStart = now.atZone(ZoneId.systemDefault()).toLocalDate()
        .atStartOfDay(ZoneId.systemDefault()).toInstant();
    Map<Long, Long> todaySeconds = new HashMap<>();
    for (Map<String, Object> row : sessionReport(todayStart, now, null, null)) {
      Long cabinetId = ((Number) row.get("cabinetId")).longValue();
      todaySeconds.put(cabinetId, todaySeconds.getOrDefault(cabinetId, 0L)
          + ((Number) row.get("durationSeconds")).longValue());
    }
    Map<Long, CabinetBinding> bindingByCabinet = new HashMap<>();
    for (CabinetBinding binding : bindings.findAllByOrderByCreatedAtAsc()) {
      bindingByCabinet.put(binding.getScreenCabinetId(), binding);
    }
    Map<Long, StudentActivitySession> activeByCabinet = new HashMap<>();
    for (StudentActivitySession session :
        sessions.findByStatusAndExpiresAtAfter(StudentActivityStatus.ACTIVE, now)) {
      StudentActivitySession existing = activeByCabinet.get(session.getCabinetId());
      if (existing == null || session.getLastActivityAt().isAfter(existing.getLastActivityAt())) {
        activeByCabinet.put(session.getCabinetId(), session);
      }
    }
    List<Map<String, Object>> result = new ArrayList<>();
    cabinets.findAllByOrderByIdAsc().forEach(cabinet -> {
      CabinetBinding binding = bindingByCabinet.get(cabinet.getId());
      StudentActivitySession active = activeByCabinet.get(cabinet.getId());
      Map<String, Object> row = new LinkedHashMap<>();
      row.put("cabinetId", cabinet.getId());
      row.put("cabinetName", cabinet.getName());
      row.put("bound", binding != null);
      row.put("deviceBindId", binding == null ? null : binding.getBindId());
      row.put("bindLabel", binding == null ? null : binding.getBindLabel());
      row.put("online", active != null);
      row.put("todayDurationSeconds", todaySeconds.getOrDefault(cabinet.getId(), 0L));
      if (active != null) {
        row.put("userId", active.getUserId());
        users.findById(active.getUserId()).ifPresent(user -> {
          row.put("studentNo", user.getStudentNo());
          row.put("studentName", user.getDisplayName());
        });
        row.put("sessionStartedAt", active.getStartedAt());
        row.put("lastActivityAt", active.getLastActivityAt());
        row.put("expiresAt", active.getExpiresAt());
        row.put("currentOnlineSeconds", seconds(active.getStartedAt(), now));
      }
      result.add(row);
    });
    return result;
  }

  @Transactional(value = "businessTransactionManager", readOnly = true)
  public List<Map<String, Object>> cabinetUsageSummary(Instant from, Instant to) {
    Map<Long, Map<String, Object>> grouped = new LinkedHashMap<>();
    for (Map<String, Object> session : sessionReport(from, to, null, null)) {
      Long cabinetId = ((Number) session.get("cabinetId")).longValue();
      Map<String, Object> row = grouped.computeIfAbsent(cabinetId, ignored -> {
        Map<String, Object> value = new LinkedHashMap<>();
        value.put("cabinetId", cabinetId);
        value.put("cabinetName", session.get("cabinetName"));
        value.put("durationSeconds", 0L);
        value.put("sessionCount", 0);
        value.put("studentCount", 0);
        value.put("studentIds", new java.util.HashSet<Long>());
        return value;
      });
      row.put("durationSeconds", (Long) row.get("durationSeconds")
          + ((Number) session.get("durationSeconds")).longValue());
      row.put("sessionCount", (Integer) row.get("sessionCount") + 1);
      @SuppressWarnings("unchecked")
      java.util.Set<Long> studentIds = (java.util.Set<Long>) row.get("studentIds");
      studentIds.add(((Number) session.get("userId")).longValue());
      row.put("studentCount", studentIds.size());
    }
    grouped.values().forEach(row -> row.remove("studentIds"));
    return new ArrayList<>(grouped.values());
  }

  @Transactional(value = "businessTransactionManager", readOnly = true)
  public Map<String, Object> cabinetHistory(Long cabinetId, Instant from, Instant to,
      Long userId, int page, int size, boolean durationAscending) {
    int safeSize = Math.min(Math.max(size, 1), 100);
    Sort.Direction direction = durationAscending ? Sort.Direction.ASC : Sort.Direction.DESC;
    Page<StudentActivitySession> data = sessions.findCabinetHistory(cabinetId, from, to, userId,
        PageRequest.of(Math.max(page, 0), safeSize,
            Sort.by(direction, "durationSeconds").and(Sort.by(Sort.Direction.DESC, "startedAt"))));
    List<Map<String, Object>> content = new ArrayList<>();
    for (StudentActivitySession session : data.getContent()) {
      content.add(toSessionRow(session, from, to));
    }
    Map<String, Object> response = new LinkedHashMap<>();
    response.put("content", content);
    response.put("page", data.getNumber());
    response.put("size", data.getSize());
    response.put("totalElements", data.getTotalElements());
    response.put("totalPages", data.getTotalPages());
    return response;
  }

  private Map<String, Object> toSessionRow(StudentActivitySession session, Instant from, Instant to) {
    Instant start = session.getStartedAt().isAfter(from) ? session.getStartedAt() : from;
    Instant effectiveEnd = effectiveEnd(session, Instant.now());
    Instant end = effectiveEnd.isBefore(to) ? effectiveEnd : to;
    Map<String, Object> row = new LinkedHashMap<>();
    row.put("id", session.getId());
    row.put("userId", session.getUserId());
    users.findById(session.getUserId()).ifPresent(user -> {
      row.put("studentNo", user.getStudentNo());
      row.put("studentName", user.getDisplayName());
    });
    row.put("startedAt", session.getStartedAt());
    row.put("endedAt", session.getEndedAt());
    row.put("lastActivityAt", session.getLastActivityAt());
    row.put("status", session.getStatus());
    row.put("bindLabel", session.getBindLabelSnapshot());
    row.put("durationSeconds", seconds(start, end));
    return row;
  }

  private void close(StudentActivitySession session, Instant endedAt, StudentActivityStatus status) {
    session.setEndedAt(endedAt);
    session.setExpiresAt(endedAt);
    session.setDurationSeconds(seconds(session.getStartedAt(), endedAt));
    session.setStatus(status);
    sessions.save(session);
  }

  private static Instant effectiveEnd(StudentActivitySession session, Instant now) {
    if (session.getEndedAt() != null) return session.getEndedAt();
    return session.getExpiresAt().isBefore(now) ? session.getExpiresAt() : now;
  }

  private static long seconds(Instant start, Instant end) {
    return Math.max(0, Duration.between(start, end).getSeconds());
  }

  private static long valueOrZero(Long value) {
    return value == null ? 0L : value;
  }

  private static String normalize(String value) {
    if (value == null || value.trim().isEmpty()) return null;
    String normalized = value.trim();
    return normalized.length() <= 64 ? normalized : null;
  }
}
