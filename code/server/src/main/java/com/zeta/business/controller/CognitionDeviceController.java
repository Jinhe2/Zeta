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
public class CognitionDeviceController {

  private final CognitionDeviceService cognitionDeviceService;
  private final AuthService authService;

  public CognitionDeviceController(
      CognitionDeviceService cognitionDeviceService, AuthService authService) {
    this.cognitionDeviceService = cognitionDeviceService;
    this.authService = authService;
  }

  @GetMapping("/api/cabinet-display-items/{itemId}/cognition-devices")
  public List<CognitionDeviceAdminResponse> listByCabinetDisplayItem(
      @RequestHeader(value = "Authorization", required = false) String authorization,
      @PathVariable Long itemId) {
    authService.requireRole(authorization, UserRole.ADMIN);
    return cognitionDeviceService.listByCabinetDisplayItem(itemId);
  }

  @PostMapping("/api/cabinet-display-items/{itemId}/cognition-devices")
  public CognitionDeviceAdminResponse create(
      @RequestHeader(value = "Authorization", required = false) String authorization,
      @PathVariable Long itemId,
      @Valid @RequestBody CreateCognitionDeviceRequest request) {
    authService.requireRole(authorization, UserRole.ADMIN);
    return cognitionDeviceService.create(itemId, request);
  }

  @GetMapping("/api/admin/cognition-devices/{id}")
  public CognitionDeviceAdminResponse get(
      @RequestHeader(value = "Authorization", required = false) String authorization,
      @PathVariable Long id) {
    authService.requireRole(authorization, UserRole.ADMIN);
    return cognitionDeviceService.getAdmin(id);
  }

  @PutMapping("/api/admin/cognition-devices/{id}")
  public CognitionDeviceAdminResponse update(
      @RequestHeader(value = "Authorization", required = false) String authorization,
      @PathVariable Long id,
      @Valid @RequestBody UpdateCognitionDeviceRequest request) {
    authService.requireRole(authorization, UserRole.ADMIN);
    return cognitionDeviceService.update(id, request);
  }

  @DeleteMapping("/api/admin/cognition-devices/{id}")
  public void delete(
      @RequestHeader(value = "Authorization", required = false) String authorization,
      @PathVariable Long id) {
    authService.requireRole(authorization, UserRole.ADMIN);
    cognitionDeviceService.delete(id);
  }
}
