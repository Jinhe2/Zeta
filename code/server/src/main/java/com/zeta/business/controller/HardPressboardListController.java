package com.zeta.business.controller;

import com.zeta.business.auth.AuthService;
import com.zeta.business.entities.pressboardselection.PressboardSelectionSaveRequest;
import com.zeta.business.entities.hardpressboardlist.HardPressboardDtos.*;
import com.zeta.business.entities.settinglist.SettingListScopeType;
import com.zeta.business.entities.user.UserRole;
import com.zeta.business.service.*;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import javax.validation.Valid;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/admin/hard-pressboard-lists")
public class HardPressboardListController {
  private final AuthService authService;
  private final HardPressboardListService listService;
  private final HardPressboardComparisonService comparisonService;
  private final HardPressboardListExcelService excelService;

  public HardPressboardListController(
      AuthService authService, HardPressboardListService listService,
      HardPressboardComparisonService comparisonService,
      HardPressboardListExcelService excelService) {
    this.authService = authService;
    this.listService = listService;
    this.comparisonService = comparisonService;
    this.excelService = excelService;
  }

  @GetMapping("/{scopeType}/{scopeId}")
  public ListResponse get(@RequestHeader(value = "Authorization", required = false) String authorization,
      @PathVariable SettingListScopeType scopeType, @PathVariable Long scopeId) {
    requireAdmin(authorization);
    return listService.get(scopeType, scopeId);
  }

  @PutMapping("/{scopeType}/{scopeId}")
  public ListResponse replace(@RequestHeader(value = "Authorization", required = false) String authorization,
      @PathVariable SettingListScopeType scopeType, @PathVariable Long scopeId,
      @Valid @RequestBody SaveRequest request) {
    requireAdmin(authorization);
    return listService.replace(scopeType, scopeId, request.getItems());
  }

  @DeleteMapping("/{scopeType}/{scopeId}")
  public ListResponse clear(@RequestHeader(value = "Authorization", required = false) String authorization,
      @PathVariable SettingListScopeType scopeType, @PathVariable Long scopeId) {
    requireAdmin(authorization);
    return listService.clear(scopeType, scopeId);
  }

  @PostMapping("/{scopeType}/{scopeId}/summon")
  public SummonResponse summon(@RequestHeader(value = "Authorization", required = false) String authorization,
      @PathVariable SettingListScopeType scopeType, @PathVariable Long scopeId) {
    requireAdmin(authorization);
    return comparisonService.summonPreview(scopeType, scopeId);
  }

  @GetMapping("/{scopeType}/{scopeId}/export")
  public ResponseEntity<byte[]> exportExcel(
      @RequestHeader(value = "Authorization", required = false) String authorization,
      @PathVariable SettingListScopeType scopeType, @PathVariable Long scopeId) {
    requireAdmin(authorization);
    String name;
    try {
      name = URLEncoder.encode("硬压板基准清单.xlsx", StandardCharsets.UTF_8.name()).replace("+", "%20");
    } catch (java.io.UnsupportedEncodingException ex) {
      name = "hard-pressboard-list.xlsx";
    }
    return ResponseEntity.ok()
        .contentType(MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
        .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename*=UTF-8''" + name)
        .body(excelService.exportWorkbook(scopeType, scopeId));
  }

  @PostMapping(value = "/{scopeType}/{scopeId}/import", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
  public ListResponse importExcel(
      @RequestHeader(value = "Authorization", required = false) String authorization,
      @PathVariable SettingListScopeType scopeType, @PathVariable Long scopeId,
      @RequestParam("file") MultipartFile file) {
    requireAdmin(authorization);
    return excelService.importWorkbook(scopeType, scopeId, file);
  }

  @PutMapping("/{scopeType}/{scopeId}/selection")
  public ListResponse saveSelection(
      @RequestHeader(value = "Authorization", required = false) String authorization,
      @PathVariable SettingListScopeType scopeType, @PathVariable Long scopeId,
      @Valid @RequestBody PressboardSelectionSaveRequest request) {
    requireAdmin(authorization);
    return listService.saveSelection(scopeType, scopeId, request.getPressboardRefs());
  }

  private void requireAdmin(String authorization) {
    authService.requireRole(authorization, UserRole.ADMIN);
  }
}
