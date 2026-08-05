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
public class DrawingLearningController {

  private final DrawingLearningService drawingLearningService;
  private final AuthService authService;

  public DrawingLearningController(
      DrawingLearningService drawingLearningService, AuthService authService) {
    this.drawingLearningService = drawingLearningService;
    this.authService = authService;
  }

  @GetMapping("/api/admin/drawing-learning/cabinets/{cabinetId}/groups")
  public List<DrawingGroupAdminResponse> listGroups(
      @RequestHeader(value = "Authorization", required = false) String authorization,
      @PathVariable Long cabinetId) {
    authService.requireRole(authorization, UserRole.ADMIN);
    return drawingLearningService.listGroups(cabinetId);
  }

  @PostMapping("/api/admin/drawing-learning/cabinets/{cabinetId}/groups")
  public DrawingGroupAdminResponse createGroup(
      @RequestHeader(value = "Authorization", required = false) String authorization,
      @PathVariable Long cabinetId,
      @Valid @RequestBody CreateDrawingGroupRequest request) {
    authService.requireRole(authorization, UserRole.ADMIN);
    return drawingLearningService.createGroup(cabinetId, request);
  }

  @GetMapping("/api/admin/drawing-learning/groups/{id}")
  public DrawingGroupAdminResponse getGroup(
      @RequestHeader(value = "Authorization", required = false) String authorization,
      @PathVariable Long id) {
    authService.requireRole(authorization, UserRole.ADMIN);
    return drawingLearningService.getGroup(id);
  }

  @PutMapping("/api/admin/drawing-learning/groups/{id}")
  public DrawingGroupAdminResponse updateGroup(
      @RequestHeader(value = "Authorization", required = false) String authorization,
      @PathVariable Long id,
      @Valid @RequestBody UpdateDrawingGroupRequest request) {
    authService.requireRole(authorization, UserRole.ADMIN);
    return drawingLearningService.updateGroup(id, request);
  }

  @DeleteMapping("/api/admin/drawing-learning/groups/{id}")
  public void deleteGroup(
      @RequestHeader(value = "Authorization", required = false) String authorization,
      @PathVariable Long id) {
    authService.requireRole(authorization, UserRole.ADMIN);
    drawingLearningService.deleteGroup(id);
  }

  @GetMapping("/api/admin/drawing-learning/groups/{groupId}/pages")
  public List<DrawingPageAdminResponse> listPages(
      @RequestHeader(value = "Authorization", required = false) String authorization,
      @PathVariable Long groupId) {
    authService.requireRole(authorization, UserRole.ADMIN);
    return drawingLearningService.listPages(groupId);
  }

  @PostMapping("/api/admin/drawing-learning/groups/{groupId}/pages")
  public DrawingPageAdminResponse createPage(
      @RequestHeader(value = "Authorization", required = false) String authorization,
      @PathVariable Long groupId,
      @Valid @RequestBody CreateDrawingPageRequest request) {
    authService.requireRole(authorization, UserRole.ADMIN);
    return drawingLearningService.createPage(groupId, request);
  }

  @GetMapping("/api/admin/drawing-learning/pages/{id}")
  public DrawingPageAdminResponse getPage(
      @RequestHeader(value = "Authorization", required = false) String authorization,
      @PathVariable Long id) {
    authService.requireRole(authorization, UserRole.ADMIN);
    return drawingLearningService.getPage(id);
  }

  @PutMapping("/api/admin/drawing-learning/pages/{id}")
  public DrawingPageAdminResponse updatePage(
      @RequestHeader(value = "Authorization", required = false) String authorization,
      @PathVariable Long id,
      @Valid @RequestBody UpdateDrawingPageRequest request) {
    authService.requireRole(authorization, UserRole.ADMIN);
    return drawingLearningService.updatePage(id, request);
  }

  @DeleteMapping("/api/admin/drawing-learning/pages/{id}")
  public void deletePage(
      @RequestHeader(value = "Authorization", required = false) String authorization,
      @PathVariable Long id) {
    authService.requireRole(authorization, UserRole.ADMIN);
    drawingLearningService.deletePage(id);
  }

  @GetMapping("/api/admin/drawing-learning/pages/{pageId}/items")
  public List<DrawingCognitionItemResponse> listItems(
      @RequestHeader(value = "Authorization", required = false) String authorization,
      @PathVariable Long pageId) {
    authService.requireRole(authorization, UserRole.ADMIN);
    return drawingLearningService.listItems(pageId);
  }

  @PostMapping("/api/admin/drawing-learning/pages/{pageId}/items")
  public DrawingCognitionItemResponse createItem(
      @RequestHeader(value = "Authorization", required = false) String authorization,
      @PathVariable Long pageId,
      @Valid @RequestBody CreateDrawingCognitionItemRequest request) {
    authService.requireRole(authorization, UserRole.ADMIN);
    return drawingLearningService.createItem(pageId, request);
  }

  @PutMapping("/api/admin/drawing-learning/items/{id}")
  public DrawingCognitionItemResponse updateItem(
      @RequestHeader(value = "Authorization", required = false) String authorization,
      @PathVariable Long id,
      @Valid @RequestBody UpdateDrawingCognitionItemRequest request) {
    authService.requireRole(authorization, UserRole.ADMIN);
    return drawingLearningService.updateItem(id, request);
  }

  @DeleteMapping("/api/admin/drawing-learning/items/{id}")
  public void deleteItem(
      @RequestHeader(value = "Authorization", required = false) String authorization,
      @PathVariable Long id) {
    authService.requireRole(authorization, UserRole.ADMIN);
    drawingLearningService.deleteItem(id);
  }

  @GetMapping("/api/knowledge/cabinets/{cabinetId}/drawing-groups")
  public List<DrawingGroupSummaryResponse> listLearnerGroups(
      @RequestHeader(value = "Authorization", required = false) String authorization,
      @PathVariable Long cabinetId) {
    authService.requireUser(authorization);
    return drawingLearningService.listEnabledGroupSummaries(cabinetId);
  }

  @GetMapping("/api/knowledge/drawing-groups/{groupId}")
  public DrawingGroupDetailResponse getLearnerGroup(
      @RequestHeader(value = "Authorization", required = false) String authorization,
      @PathVariable Long groupId) {
    authService.requireUser(authorization);
    return drawingLearningService.getEnabledGroupDetail(groupId);
  }
}
