package com.zeta.business.controller;

import com.zeta.business.auth.AuthService;
import com.zeta.business.entities.user.User;
import com.zeta.business.entities.user.UserRole;
import com.zeta.business.service.LearningPathService;
import java.util.List;
import java.util.Map;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/learning-path")
public class LearningPathController {
  private final AuthService auth;
  private final LearningPathService paths;

  public LearningPathController(AuthService auth, LearningPathService paths) {
    this.auth = auth;
    this.paths = paths;
  }

  @GetMapping("/student")
  public Map<String, Object> student(@RequestHeader("Authorization") String authorization) {
    User user = auth.requireRole(authorization, UserRole.STUDENT);
    return paths.studentPath(user.getId());
  }

  @PostMapping("/student/modules/{code}/visit")
  public Map<String, Object> visit(@RequestHeader("Authorization") String authorization,
      @PathVariable String code) {
    User user = auth.requireRole(authorization, UserRole.STUDENT);
    return paths.visit(user.getId(), code);
  }

  @PostMapping("/student/modules/{code}/complete")
  public Map<String, Object> complete(@RequestHeader("Authorization") String authorization,
      @PathVariable String code) {
    User user = auth.requireRole(authorization, UserRole.STUDENT);
    return paths.complete(user.getId(), code);
  }

  @GetMapping("/config")
  public List<Map<String, Object>> config(@RequestHeader("Authorization") String authorization) {
    auth.requireRole(authorization, UserRole.ADMIN);
    return paths.config();
  }

  @PutMapping("/config")
  public List<Map<String, Object>> save(@RequestHeader("Authorization") String authorization,
      @RequestBody List<Map<String, Object>> body) {
    auth.requireRole(authorization, UserRole.ADMIN);
    return paths.saveConfig(body);
  }

  @GetMapping("/monitor")
  public List<Map<String, Object>> monitor(@RequestHeader("Authorization") String authorization) {
    auth.requireAnyRole(authorization, UserRole.ADMIN, UserRole.TEACHER);
    return paths.monitor();
  }
}
