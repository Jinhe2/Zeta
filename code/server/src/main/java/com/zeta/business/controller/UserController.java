package com.zeta.business.controller;

import com.zeta.business.auth.*;
import com.zeta.business.auth.AuthService;
import com.zeta.business.auth.dto.*;
import com.zeta.business.entities.binding.*;
import com.zeta.business.entities.binding.dto.*;
import com.zeta.business.entities.cabinetdisplay.*;
import com.zeta.business.entities.cabinetdisplay.dto.*;
import com.zeta.business.entities.cognitiondevice.*;
import com.zeta.business.entities.cognitiondevice.dto.*;
import com.zeta.business.entities.devicedisplay.*;
import com.zeta.business.entities.devicedisplay.dto.*;
import com.zeta.business.entities.drawinglearning.*;
import com.zeta.business.entities.drawinglearning.dto.*;
import com.zeta.business.entities.learningresource.*;
import com.zeta.business.entities.learningresource.dto.*;
import com.zeta.business.entities.logiclearning.*;
import com.zeta.business.entities.logiclearning.dto.*;
import com.zeta.business.entities.logicnodecognition.*;
import com.zeta.business.entities.logicnodecognition.dto.*;
import com.zeta.business.entities.monitor.*;
import com.zeta.business.entities.snapshot.*;
import com.zeta.business.entities.snapshot.dto.*;
import com.zeta.business.entities.user.*;
import com.zeta.business.entities.user.User;
import com.zeta.business.entities.user.UserRole;
import com.zeta.business.entities.user.dto.*;
import com.zeta.business.entities.user.dto.CreateUserRequest;
import com.zeta.business.entities.user.dto.ResetPasswordRequest;
import com.zeta.business.entities.user.dto.UpdateUserRequest;
import com.zeta.business.entities.user.dto.UserSummaryResponse;
import com.zeta.business.media.*;
import com.zeta.business.service.*;
import com.zeta.business.service.UserService;
import com.zeta.business.storage.*;
import java.util.List;
import javax.validation.Valid;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/users")
@Validated
public class UserController {

  private final UserService userService;
  private final AuthService authService;

  public UserController(UserService userService, AuthService authService) {
    this.userService = userService;
    this.authService = authService;
  }

  @GetMapping
  public List<UserSummaryResponse> list(
      @RequestHeader(value = "Authorization", required = false) String authorization,
      @RequestParam("role") UserRole role) {
    User operator = authService.requireUser(authorization);
    requireUserManagementPermission(operator, role);
    return userService.listUsers(role);
  }

  @GetMapping("/{id}")
  public UserSummaryResponse detail(
      @RequestHeader(value = "Authorization", required = false) String authorization,
      @PathVariable Long id) {
    User operator = authService.requireUser(authorization);
    UserSummaryResponse user = userService.getUser(id);
    requireUserManagementPermission(operator, user.getRole());
    return user;
  }

  @PostMapping
  public UserSummaryResponse create(
      @RequestHeader(value = "Authorization", required = false) String authorization,
      @Valid @RequestBody CreateUserRequest request) {
    User operator = authService.requireUser(authorization);
    requireUserManagementPermission(operator, request.getRole());
    return userService.createUser(request);
  }

  @PostMapping("/batch-import-students")
  public BatchImportStudentsResponse batchImportStudents(
      @RequestHeader(value = "Authorization", required = false) String authorization,
      @Valid @RequestBody BatchImportStudentsRequest request) {
    User operator = authService.requireUser(authorization);
    requireUserManagementPermission(operator, UserRole.STUDENT);
    return userService.batchImportStudents(request);
  }

  @PostMapping("/batch-import/{role}")
  public BatchImportUsersResponse batchImportUsers(
      @RequestHeader(value = "Authorization", required = false) String authorization,
      @PathVariable UserRole role,
      @Valid @RequestBody BatchImportUsersRequest request) {
    User operator = authService.requireUser(authorization);
    requireBatchImportRole(role);
    requireUserManagementPermission(operator, role);
    return userService.batchImportUsers(request, role);
  }

  @PutMapping("/{id}")
  public UserSummaryResponse update(
      @RequestHeader(value = "Authorization", required = false) String authorization,
      @PathVariable Long id,
      @Valid @RequestBody UpdateUserRequest request) {
    User operator = authService.requireUser(authorization);
    requireUserManagementPermission(operator, request.getRole());
    requireTargetRole(id, request.getRole());
    return userService.updateUser(id, request);
  }

  @PutMapping("/{id}/password")
  public void resetPassword(
      @RequestHeader(value = "Authorization", required = false) String authorization,
      @PathVariable Long id,
      @Valid @RequestBody ResetPasswordRequest request) {
    User operator = authService.requireUser(authorization);
    UserSummaryResponse user = userService.getUser(id);
    requireUserManagementPermission(operator, user.getRole());
    userService.resetPassword(id, request.getPassword());
  }

  @DeleteMapping("/{id}")
  public void delete(
      @RequestHeader(value = "Authorization", required = false) String authorization,
      @PathVariable Long id) {
    User operator = authService.requireUser(authorization);
    UserSummaryResponse user = userService.getUser(id);
    requireUserManagementPermission(operator, user.getRole());
    userService.deleteUser(id, operator);
  }

  private void requireTargetRole(Long id, UserRole expectedRole) {
    UserSummaryResponse user = userService.getUser(id);
    if (user.getRole() != expectedRole) {
      throw new org.springframework.web.server.ResponseStatusException(
          org.springframework.http.HttpStatus.BAD_REQUEST, "不支持变更用户角色，请在对应角色管理中操作");
    }
  }

  private void requireBatchImportRole(UserRole role) {
    if (role == UserRole.STUDENT || role == UserRole.TEACHER) {
      return;
    }
    throw new org.springframework.web.server.ResponseStatusException(
        org.springframework.http.HttpStatus.BAD_REQUEST, "仅支持批量导入学员和教师账号");
  }

  private void requireUserManagementPermission(User operator, UserRole targetRole) {
    if (operator.getRole() == UserRole.ADMIN) {
      return;
    }
    if (operator.getRole() == UserRole.TEACHER && targetRole == UserRole.STUDENT) {
      return;
    }
    throw new org.springframework.web.server.ResponseStatusException(
        org.springframework.http.HttpStatus.FORBIDDEN, "教师只能管理学员账号");
  }
}
