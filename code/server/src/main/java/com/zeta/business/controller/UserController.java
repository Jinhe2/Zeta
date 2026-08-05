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
    authService.requireRole(authorization, UserRole.ADMIN);
    return userService.listUsers(role);
  }

  @GetMapping("/{id}")
  public UserSummaryResponse detail(
      @RequestHeader(value = "Authorization", required = false) String authorization,
      @PathVariable Long id) {
    authService.requireRole(authorization, UserRole.ADMIN);
    return userService.getUser(id);
  }

  @PostMapping
  public UserSummaryResponse create(
      @RequestHeader(value = "Authorization", required = false) String authorization,
      @Valid @RequestBody CreateUserRequest request) {
    authService.requireRole(authorization, UserRole.ADMIN);
    return userService.createUser(request);
  }

  @PostMapping("/batch-import-students")
  public BatchImportStudentsResponse batchImportStudents(
      @RequestHeader(value = "Authorization", required = false) String authorization,
      @Valid @RequestBody BatchImportStudentsRequest request) {
    authService.requireRole(authorization, UserRole.ADMIN);
    return userService.batchImportStudents(request);
  }

  @PutMapping("/{id}")
  public UserSummaryResponse update(
      @RequestHeader(value = "Authorization", required = false) String authorization,
      @PathVariable Long id,
      @Valid @RequestBody UpdateUserRequest request) {
    authService.requireRole(authorization, UserRole.ADMIN);
    return userService.updateUser(id, request);
  }

  @PutMapping("/{id}/password")
  public void resetPassword(
      @RequestHeader(value = "Authorization", required = false) String authorization,
      @PathVariable Long id,
      @Valid @RequestBody ResetPasswordRequest request) {
    authService.requireRole(authorization, UserRole.ADMIN);
    userService.resetPassword(id, request.getPassword());
  }

  @DeleteMapping("/{id}")
  public void delete(
      @RequestHeader(value = "Authorization", required = false) String authorization,
      @PathVariable Long id) {
    User operator = authService.requireRole(authorization, UserRole.ADMIN);
    userService.deleteUser(id, operator);
  }
}
