package com.zeta.business.controller;

import com.zeta.business.auth.AuthService;
import com.zeta.business.entities.settinglist.SettingListScopeType;
import com.zeta.business.entities.settinglist.dto.*;
import com.zeta.business.entities.user.UserRole;
import com.zeta.business.service.*;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import javax.validation.Valid;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
public class SettingListController {
  private final AuthService authService;
  private final SettingListService settingListService;
  private final SettingComparisonService comparisonService;
  private final SettingListExcelService excelService;

  public SettingListController(
      AuthService authService,
      SettingListService settingListService,
      SettingComparisonService comparisonService,
      SettingListExcelService excelService) {
    this.authService = authService;
    this.settingListService = settingListService;
    this.comparisonService = comparisonService;
    this.excelService = excelService;
  }

  @GetMapping({
      "/api/admin/setting-lists/{scopeType}/{scopeId}",
      "/api/teacher/setting-lists/{scopeType}/{scopeId}"
  })
  public SettingListResponse get(
      @RequestHeader(value = "Authorization", required = false) String authorization,
      @PathVariable SettingListScopeType scopeType,
      @PathVariable Long scopeId) {
    requireTeacherOrAdmin(authorization);
    return settingListService.get(scopeType, scopeId);
  }

  @PutMapping({
      "/api/admin/setting-lists/{scopeType}/{scopeId}",
      "/api/teacher/setting-lists/{scopeType}/{scopeId}"
  })
  public SettingListResponse replace(
      @RequestHeader(value = "Authorization", required = false) String authorization,
      @PathVariable SettingListScopeType scopeType,
      @PathVariable Long scopeId,
      @Valid @RequestBody SettingListSaveRequest request) {
    requireTeacherOrAdmin(authorization);
    return settingListService.replace(scopeType, scopeId, request.getItems());
  }

  @DeleteMapping({
      "/api/admin/setting-lists/{scopeType}/{scopeId}",
      "/api/teacher/setting-lists/{scopeType}/{scopeId}"
  })
  public SettingListResponse clear(
      @RequestHeader(value = "Authorization", required = false) String authorization,
      @PathVariable SettingListScopeType scopeType,
      @PathVariable Long scopeId) {
    requireTeacherOrAdmin(authorization);
    return settingListService.clear(scopeType, scopeId);
  }

  @PostMapping({
      "/api/admin/setting-lists/{scopeType}/{scopeId}/summon",
      "/api/teacher/setting-lists/{scopeType}/{scopeId}/summon"
  })
  public SettingSummonResponse summon(
      @RequestHeader(value = "Authorization", required = false) String authorization,
      @PathVariable SettingListScopeType scopeType,
      @PathVariable Long scopeId) {
    requireTeacherOrAdmin(authorization);
    return comparisonService.summonPreview(scopeType, scopeId);
  }

  @GetMapping({
      "/api/admin/setting-lists/{scopeType}/{scopeId}/export",
      "/api/teacher/setting-lists/{scopeType}/{scopeId}/export"
  })
  public ResponseEntity<byte[]> exportExcel(
      @RequestHeader(value = "Authorization", required = false) String authorization,
      @PathVariable SettingListScopeType scopeType,
      @PathVariable Long scopeId) {
    requireTeacherOrAdmin(authorization);
    byte[] bytes = excelService.exportWorkbook(scopeType, scopeId);
    String name;
    try {
      name = URLEncoder.encode("定值清单.xlsx", StandardCharsets.UTF_8.name()).replace("+", "%20");
    } catch (java.io.UnsupportedEncodingException ex) {
      name = "setting-list.xlsx";
    }
    return ResponseEntity.ok()
        .contentType(MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
        .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename*=UTF-8''" + name)
        .body(bytes);
  }

  @PostMapping(
      value = {
          "/api/admin/setting-lists/{scopeType}/{scopeId}/import",
          "/api/teacher/setting-lists/{scopeType}/{scopeId}/import"
      },
      consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
  public SettingListResponse importExcel(
      @RequestHeader(value = "Authorization", required = false) String authorization,
      @PathVariable SettingListScopeType scopeType,
      @PathVariable Long scopeId,
      @RequestParam("file") MultipartFile file) {
    requireTeacherOrAdmin(authorization);
    return excelService.importWorkbook(scopeType, scopeId, file);
  }

  @PutMapping({
      "/api/admin/setting-lists/{scopeType}/{scopeId}/selection",
      "/api/teacher/setting-lists/{scopeType}/{scopeId}/selection"
  })
  public SettingListResponse saveSelection(
      @RequestHeader(value = "Authorization", required = false) String authorization,
      @PathVariable SettingListScopeType scopeType, @PathVariable Long scopeId,
      @Valid @RequestBody SettingSelectionSaveRequest request) {
    requireTeacherOrAdmin(authorization);
    return settingListService.saveSelection(scopeType, scopeId, request.getSettingRefs());
  }

  @PostMapping("/api/setting-lists/check")
  public SettingCheckResponse check(
      @RequestHeader(value = "Authorization", required = false) String authorization,
      @RequestBody java.util.Map<String, Object> body) {
    authService.requireUser(authorization);
    Object id = body.get("logicDiagramId");
    if (!(id instanceof Number)) {
      throw new org.springframework.web.server.ResponseStatusException(
          HttpStatus.BAD_REQUEST, "缺少 logicDiagramId 参数");
    }
    return comparisonService.checkForLogic(((Number) id).longValue());
  }

  private void requireTeacherOrAdmin(String authorization) {
    authService.requireAnyRole(authorization, UserRole.ADMIN, UserRole.TEACHER);
  }
}
