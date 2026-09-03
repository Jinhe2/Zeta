package com.zeta.business.controller;

import com.zeta.business.auth.AuthService;
import com.zeta.business.entities.pressboardselection.PressboardSelectionSaveRequest;
import com.zeta.business.entities.settinglist.SettingListScopeType;
import com.zeta.business.entities.softpressboardlist.SoftPressboardDtos.*;
import com.zeta.business.entities.user.UserRole;
import com.zeta.business.service.*;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import javax.validation.Valid;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
public class SoftPressboardListController {
  private final AuthService authService;
  private final SoftPressboardListService listService;
  private final SoftPressboardComparisonService comparisonService;
  private final SoftPressboardListExcelService excelService;

  public SoftPressboardListController(
      AuthService authService, SoftPressboardListService listService,
      SoftPressboardComparisonService comparisonService,
      SoftPressboardListExcelService excelService) {
    this.authService = authService;
    this.listService = listService;
    this.comparisonService = comparisonService;
    this.excelService = excelService;
  }

  @GetMapping({
      "/api/admin/soft-pressboard-lists/{scopeType}/{scopeId}",
      "/api/teacher/soft-pressboard-lists/{scopeType}/{scopeId}"
  })
  public ListResponse get(@RequestHeader(value = "Authorization", required = false) String authorization,
      @PathVariable SettingListScopeType scopeType, @PathVariable Long scopeId) {
    requireTeacherOrAdmin(authorization);
    return listService.get(scopeType, scopeId);
  }

  @PutMapping({
      "/api/admin/soft-pressboard-lists/{scopeType}/{scopeId}",
      "/api/teacher/soft-pressboard-lists/{scopeType}/{scopeId}"
  })
  public ListResponse replace(@RequestHeader(value = "Authorization", required = false) String authorization,
      @PathVariable SettingListScopeType scopeType, @PathVariable Long scopeId,
      @Valid @RequestBody SaveRequest request) {
    requireTeacherOrAdmin(authorization);
    return listService.replace(scopeType, scopeId, request.getItems());
  }

  @DeleteMapping({
      "/api/admin/soft-pressboard-lists/{scopeType}/{scopeId}",
      "/api/teacher/soft-pressboard-lists/{scopeType}/{scopeId}"
  })
  public ListResponse clear(@RequestHeader(value = "Authorization", required = false) String authorization,
      @PathVariable SettingListScopeType scopeType, @PathVariable Long scopeId) {
    requireTeacherOrAdmin(authorization);
    return listService.clear(scopeType, scopeId);
  }

  @PostMapping({
      "/api/admin/soft-pressboard-lists/{scopeType}/{scopeId}/summon",
      "/api/teacher/soft-pressboard-lists/{scopeType}/{scopeId}/summon"
  })
  public SummonResponse summon(@RequestHeader(value = "Authorization", required = false) String authorization,
      @PathVariable SettingListScopeType scopeType, @PathVariable Long scopeId) {
    requireTeacherOrAdmin(authorization);
    return comparisonService.summonPreview(scopeType, scopeId);
  }

  @GetMapping({
      "/api/admin/soft-pressboard-lists/{scopeType}/{scopeId}/export",
      "/api/teacher/soft-pressboard-lists/{scopeType}/{scopeId}/export"
  })
  public ResponseEntity<byte[]> exportExcel(
      @RequestHeader(value = "Authorization", required = false) String authorization,
      @PathVariable SettingListScopeType scopeType, @PathVariable Long scopeId) {
    requireTeacherOrAdmin(authorization);
    String name;
    try {
      name = URLEncoder.encode("软压板基准清单.xlsx", StandardCharsets.UTF_8.name()).replace("+", "%20");
    } catch (java.io.UnsupportedEncodingException ex) {
      name = "soft-pressboard-list.xlsx";
    }
    return ResponseEntity.ok()
        .contentType(MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
        .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename*=UTF-8''" + name)
        .body(excelService.exportWorkbook(scopeType, scopeId));
  }

  @PostMapping(
      value = {
          "/api/admin/soft-pressboard-lists/{scopeType}/{scopeId}/import",
          "/api/teacher/soft-pressboard-lists/{scopeType}/{scopeId}/import"
      },
      consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
  public ListResponse importExcel(
      @RequestHeader(value = "Authorization", required = false) String authorization,
      @PathVariable SettingListScopeType scopeType, @PathVariable Long scopeId,
      @RequestParam("file") MultipartFile file) {
    requireTeacherOrAdmin(authorization);
    return excelService.importWorkbook(scopeType, scopeId, file);
  }

  @PutMapping({"/api/admin/soft-pressboard-lists/{scopeType}/{scopeId}/selection",
      "/api/teacher/soft-pressboard-lists/{scopeType}/{scopeId}/selection"})
  public ListResponse saveSelection(
      @RequestHeader(value = "Authorization", required = false) String authorization,
      @PathVariable SettingListScopeType scopeType, @PathVariable Long scopeId,
      @Valid @RequestBody PressboardSelectionSaveRequest request) {
    requireTeacherOrAdmin(authorization);
    return listService.saveSelection(scopeType, scopeId, request.getPressboardRefs());
  }

  private void requireTeacherOrAdmin(String authorization) {
    authService.requireAnyRole(authorization, UserRole.ADMIN, UserRole.TEACHER);
  }
}
