package com.zeta.business.user;

import com.zeta.business.user.User;
import com.zeta.business.user.UserRole;
import com.zeta.business.auth.AuthService;
import com.zeta.business.user.UserService;
import com.zeta.business.user.CreateUserRequest;
import com.zeta.business.user.ResetPasswordRequest;
import com.zeta.business.user.UpdateUserRequest;
import com.zeta.business.user.UserSummaryResponse;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import java.util.List;

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
