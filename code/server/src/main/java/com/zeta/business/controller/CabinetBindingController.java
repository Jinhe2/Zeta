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
import java.util.Collections;
import java.util.List;
import java.util.Map;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/bindings")
public class CabinetBindingController {

  private final CabinetBindingService bindingService;
  private final AuthService authService;

  public CabinetBindingController(CabinetBindingService bindingService, AuthService authService) {
    this.bindingService = bindingService;
    this.authService = authService;
  }

  /** 查询当前设备绑定状态（任何已登录用户可调用） */
  @GetMapping("/check")
  public BindingCheckResponse check(
      @RequestHeader("Authorization") String authorization, @RequestParam String bindId) {
    authService.requireUser(authorization);
    return bindingService.checkBinding(bindId);
  }

  /** 管理员：屏柜绑定清单 */
  @GetMapping("/cabinets")
  public List<BindingListResponse> listCabinets(
      @RequestHeader("Authorization") String authorization) {
    authService.requireRole(authorization, UserRole.ADMIN);
    return bindingService.listAllCabinetsWithBinding();
  }

  /** 管理员：绑定屏柜 */
  @PostMapping("/cabinets/{cabinetId}")
  public BindingListResponse bind(
      @RequestHeader("Authorization") String authorization,
      @PathVariable Long cabinetId,
      @RequestBody Map<String, String> body) {
    authService.requireRole(authorization, UserRole.ADMIN);
    String bindId = body.get("bindId");
    String bindLabel = body.get("bindLabel");
    if (bindId == null || bindId.trim().isEmpty()) {
      throw new org.springframework.web.server.ResponseStatusException(
          org.springframework.http.HttpStatus.BAD_REQUEST, "bindId 不能为空");
    }
    if (bindLabel == null || bindLabel.trim().isEmpty()) {
      throw new org.springframework.web.server.ResponseStatusException(
          org.springframework.http.HttpStatus.BAD_REQUEST, "bindLabel 不能为空");
    }
    return bindingService.bindCabinet(cabinetId, bindId.trim(), bindLabel.trim());
  }

  /** 管理员：解绑屏柜 */
  @DeleteMapping("/cabinets/{cabinetId}")
  public Map<String, String> unbind(
      @RequestHeader("Authorization") String authorization, @PathVariable Long cabinetId) {
    authService.requireRole(authorization, UserRole.ADMIN);
    bindingService.unbindCabinet(cabinetId);
    return Collections.singletonMap("message", "解绑成功");
  }

  /** 管理员：强制解绑屏柜 */
  @DeleteMapping("/cabinets/{cabinetId}/force")
  public Map<String, String> forceUnbind(
      @RequestHeader("Authorization") String authorization, @PathVariable Long cabinetId) {
    authService.requireRole(authorization, UserRole.ADMIN);
    bindingService.forceUnbindCabinet(cabinetId);
    return Collections.singletonMap("message", "强制解绑成功");
  }
}
