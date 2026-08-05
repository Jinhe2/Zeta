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
public class CabinetDisplayItemController {

  private final CabinetDisplayItemService displayItemService;
  private final AuthService authService;

  public CabinetDisplayItemController(
      CabinetDisplayItemService displayItemService, AuthService authService) {
    this.displayItemService = displayItemService;
    this.authService = authService;
  }

  @GetMapping("/api/cabinets/{cabinetId}/display-items")
  public List<CabinetDisplayItemAdminResponse> listByCabinet(
      @RequestHeader(value = "Authorization", required = false) String authorization,
      @PathVariable Long cabinetId) {
    authService.requireRole(authorization, UserRole.ADMIN);
    return displayItemService.listByScreenCabinet(cabinetId);
  }

  @PostMapping("/api/cabinets/{cabinetId}/display-items")
  public CabinetDisplayItemAdminResponse create(
      @RequestHeader(value = "Authorization", required = false) String authorization,
      @PathVariable Long cabinetId,
      @Valid @RequestBody CreateCabinetDisplayItemRequest request) {
    authService.requireRole(authorization, UserRole.ADMIN);
    return displayItemService.create(cabinetId, request);
  }

  @GetMapping("/api/cabinet-display-items/{id}")
  public CabinetDisplayItemAdminResponse get(
      @RequestHeader(value = "Authorization", required = false) String authorization,
      @PathVariable Long id) {
    authService.requireRole(authorization, UserRole.ADMIN);
    return displayItemService.getAdmin(id);
  }

  @PutMapping("/api/admin/cabinet-display-items/{id}")
  public CabinetDisplayItemAdminResponse update(
      @RequestHeader(value = "Authorization", required = false) String authorization,
      @PathVariable Long id,
      @Valid @RequestBody UpdateCabinetDisplayItemRequest request) {
    authService.requireRole(authorization, UserRole.ADMIN);
    return displayItemService.update(id, request);
  }

  @DeleteMapping("/api/admin/cabinet-display-items/{id}")
  public void delete(
      @RequestHeader(value = "Authorization", required = false) String authorization,
      @PathVariable Long id) {
    authService.requireRole(authorization, UserRole.ADMIN);
    displayItemService.delete(id);
  }
}
