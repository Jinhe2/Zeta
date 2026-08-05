package com.zeta.business.controller;

import com.zeta.business.auth.*;
import com.zeta.business.auth.AuthService;
import com.zeta.business.auth.dto.*;
import com.zeta.business.auth.dto.AuthTokenResponse;
import com.zeta.business.auth.dto.ChangePasswordRequest;
import com.zeta.business.auth.dto.LoginRequest;
import com.zeta.business.auth.dto.RefreshTokenRequest;
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
import com.zeta.business.entities.user.dto.*;
import com.zeta.business.entities.user.dto.UserProfileResponse;
import com.zeta.business.media.*;
import com.zeta.business.service.*;
import com.zeta.business.storage.*;
import javax.validation.Valid;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
@Validated
public class AuthController {

  private final AuthService authService;

  public AuthController(AuthService authService) {
    this.authService = authService;
  }

  @PostMapping("/login")
  public AuthTokenResponse login(@Valid @RequestBody LoginRequest request) {
    return authService.login(request.getUsername(), request.getPassword());
  }

  @PostMapping("/refresh")
  public AuthTokenResponse refresh(@Valid @RequestBody RefreshTokenRequest request) {
    return authService.refresh(request.getRefreshToken());
  }

  @GetMapping("/me")
  public UserProfileResponse me(
      @RequestHeader(value = "Authorization", required = false) String authorization) {
    return authService.profile(authorization);
  }

  @PostMapping("/logout")
  public void logout(@Valid @RequestBody(required = false) RefreshTokenRequest request) {
    if (request != null) {
      authService.logout(request.getRefreshToken());
    }
  }

  @PostMapping("/change-password")
  public void changePassword(
      @RequestHeader(value = "Authorization", required = false) String authorization,
      @Valid @RequestBody ChangePasswordRequest request) {
    authService.changePassword(authorization, request.getOldPassword(), request.getNewPassword());
  }
}
