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
    CabinetBinding binding = bindings.findByBindId(bindId).orElse(null);
    if (binding == null) return;
    User user = users.findByIdForActivityUpdate(userId).orElse(null);
    if (user == null) return;

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
      Instant effectiveEnd = session.getEndedAt() == null ? session.getExpiresAt() : session.getEndedAt();
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

  private void close(StudentActivitySession session, Instant endedAt, StudentActivityStatus status) {
    session.setEndedAt(endedAt);
    session.setExpiresAt(endedAt);
    session.setDurationSeconds(seconds(session.getStartedAt(), endedAt));
    session.setStatus(status);
    sessions.save(session);
  }

  private static long seconds(Instant start, Instant end) {
    return Math.max(0, Duration.between(start, end).getSeconds());
  }

  private static String normalize(String value) {
    if (value == null || value.trim().isEmpty()) return null;
    String normalized = value.trim();
    return normalized.length() <= 64 ? normalized : null;
  }
}
