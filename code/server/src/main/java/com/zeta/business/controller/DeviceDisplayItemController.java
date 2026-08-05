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
import com.zeta.business.entities.user.UserRole;
import com.zeta.business.entities.user.dto.*;
import com.zeta.business.media.*;
import com.zeta.business.service.*;
import com.zeta.business.storage.*;
import java.util.List;
import javax.validation.Valid;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

@RestController
@Validated
public class DeviceDisplayItemController {

  private final DeviceDisplayItemService displayItemService;
  private final AuthService authService;

  public DeviceDisplayItemController(
      DeviceDisplayItemService displayItemService, AuthService authService) {
    this.displayItemService = displayItemService;
    this.authService = authService;
  }

  @GetMapping("/api/cognition-devices/{cognitionDeviceId}/display-items")
  public List<DeviceDisplayItemAdminResponse> listByCognitionDevice(
      @RequestHeader(value = "Authorization", required = false) String authorization,
      @PathVariable Long cognitionDeviceId) {
    authService.requireRole(authorization, UserRole.ADMIN);
    return displayItemService.listByCognitionDevice(cognitionDeviceId);
  }

  @PostMapping("/api/cognition-devices/{cognitionDeviceId}/display-items")
  public DeviceDisplayItemAdminResponse create(
      @RequestHeader(value = "Authorization", required = false) String authorization,
      @PathVariable Long cognitionDeviceId,
      @Valid @RequestBody CreateDeviceDisplayItemRequest request) {
    authService.requireRole(authorization, UserRole.ADMIN);
    return displayItemService.create(cognitionDeviceId, request);
  }

  @PutMapping("/api/admin/device-display-items/{id}")
  public DeviceDisplayItemAdminResponse update(
      @RequestHeader(value = "Authorization", required = false) String authorization,
      @PathVariable Long id,
      @Valid @RequestBody UpdateDeviceDisplayItemRequest request) {
    authService.requireRole(authorization, UserRole.ADMIN);
    return displayItemService.update(id, request);
  }

  @DeleteMapping("/api/admin/device-display-items/{id}")
  public void delete(
      @RequestHeader(value = "Authorization", required = false) String authorization,
      @PathVariable Long id) {
    authService.requireRole(authorization, UserRole.ADMIN);
    displayItemService.delete(id);
  }
}
