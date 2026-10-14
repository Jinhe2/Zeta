package com.zeta.business.controller;

import com.zeta.business.auth.AuthService;
import com.zeta.business.entities.user.User;
import com.zeta.business.entities.user.UserRole;
import com.zeta.business.service.StudentActivityService;
import java.time.Instant;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import org.springframework.web.bind.annotation.*;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api/student-activity")
public class StudentActivityController {
  private final AuthService auth;
  private final StudentActivityService activities;

  public StudentActivityController(AuthService auth, StudentActivityService activities) {
    this.auth = auth;
    this.activities = activities;
  }

  @PostMapping("/end")
  public Map<String, Boolean> end(@RequestHeader("Authorization") String authorization,
      @RequestHeader(value = "X-Device-Bind-Id", required = false) String bindId) {
    User user = auth.requireRole(authorization, UserRole.STUDENT);
    activities.endActivity(user.getId(), bindId, Instant.now());
    return Collections.singletonMap("ended", true);
  }

  @GetMapping("/sessions")
  public List<Map<String, Object>> sessions(
      @RequestHeader("Authorization") String authorization,
      @RequestParam Instant from, @RequestParam Instant to,
      @RequestParam(required = false) Long userId,
      @RequestParam(required = false) Long cabinetId) {
    auth.requireAnyRole(authorization, UserRole.ADMIN, UserRole.TEACHER);
    validateRange(from, to);
    return activities.sessionReport(from, to, userId, cabinetId);
  }

  @GetMapping("/summary")
  public List<Map<String, Object>> summary(
      @RequestHeader("Authorization") String authorization,
      @RequestParam Instant from, @RequestParam Instant to,
      @RequestParam(required = false) Long userId,
      @RequestParam(required = false) Long cabinetId) {
    auth.requireAnyRole(authorization, UserRole.ADMIN, UserRole.TEACHER);
    validateRange(from, to);
    return activities.summaryReport(from, to, userId, cabinetId);
  }

  @GetMapping("/cabinets/online")
  public List<Map<String, Object>> cabinetOnline(
      @RequestHeader("Authorization") String authorization) {
    auth.requireAnyRole(authorization, UserRole.ADMIN, UserRole.TEACHER);
    return activities.cabinetOnlineReport(Instant.now());
  }

  @GetMapping("/cabinets/usage-summary")
  public List<Map<String, Object>> cabinetUsageSummary(
      @RequestHeader("Authorization") String authorization,
      @RequestParam Instant from, @RequestParam Instant to) {
    auth.requireAnyRole(authorization, UserRole.ADMIN, UserRole.TEACHER);
    validateRange(from, to);
    return activities.cabinetUsageSummary(from, to);
  }

  @GetMapping("/cabinets/{cabinetId}/sessions")
  public Map<String, Object> cabinetHistory(
      @RequestHeader("Authorization") String authorization,
      @PathVariable Long cabinetId,
      @RequestParam Instant from, @RequestParam Instant to,
      @RequestParam(required = false) Long userId,
      @RequestParam(defaultValue = "0") int page,
      @RequestParam(defaultValue = "20") int size,
      @RequestParam(defaultValue = "desc") String durationSort) {
    auth.requireAnyRole(authorization, UserRole.ADMIN, UserRole.TEACHER);
    validateRange(from, to);
    return activities.cabinetHistory(cabinetId, from, to, userId, page, size,
        "asc".equalsIgnoreCase(durationSort));
  }

  private static void validateRange(Instant from, Instant to) {
    if (!from.isBefore(to)) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "from 必须早于 to");
    }
  }
}
