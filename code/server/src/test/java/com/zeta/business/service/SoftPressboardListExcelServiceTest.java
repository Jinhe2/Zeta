package com.zeta.business.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import com.zeta.business.entities.settinglist.SettingListScopeType;
import com.zeta.business.entities.softpressboardlist.SoftPressboardDtos.*;
import java.util.*;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.server.ResponseStatusException;

class SoftPressboardListExcelServiceTest {
  @Test
  void 导出文件可原样导回并整套替换() {
    SoftPressboardListService listService = mock(SoftPressboardListService.class);
    SoftPressboardListExcelService excelService = new SoftPressboardListExcelService(listService);
    ItemResponse item = new ItemResponse("ref-1", "差动保护软压板", false, true, 0);
    ListResponse response = response(item);
    when(listService.get(SettingListScopeType.IED_DEVICE, 2L)).thenReturn(response);
    when(listService.replace(eq(SettingListScopeType.IED_DEVICE), eq(2L), anyList()))
        .thenReturn(response);
    byte[] workbook = excelService.exportWorkbook(SettingListScopeType.IED_DEVICE, 2L);
    excelService.importWorkbook(SettingListScopeType.IED_DEVICE, 2L,
        new MockMultipartFile("file", "软压板.xlsx", "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet", workbook));
    verify(listService).replace(eq(SettingListScopeType.IED_DEVICE), eq(2L), argThat(rows ->
        rows.size() == 1 && Boolean.FALSE.equals(rows.get(0).getBaselineValue())));
  }

  @Test
  void 导入兼容中文数字和布尔文本且拒绝非法状态() throws Exception {
    SoftPressboardListService listService = mock(SoftPressboardListService.class);
    SoftPressboardListExcelService excelService = new SoftPressboardListExcelService(listService);
    when(listService.replace(any(), anyLong(), anyList())).thenReturn(response());
    for (String state : Arrays.asList("投入", "退出", "1", "0", "true", "false")) {
      excelService.importWorkbook(SettingListScopeType.IED_DEVICE, 2L,
          new MockMultipartFile("file", "a.xlsx", "application/octet-stream", workbook(state)));
    }
    verify(listService, times(6)).replace(any(), anyLong(), anyList());

    ResponseStatusException error = assertThrows(ResponseStatusException.class,
        () -> excelService.importWorkbook(SettingListScopeType.IED_DEVICE, 2L,
            new MockMultipartFile("file", "b.xlsx", "application/octet-stream", workbook("未知"))));
    assertTrue(error.getReason().contains("基准状态"));
  }

  private byte[] workbook(String state) throws Exception {
    try (Workbook workbook = new XSSFWorkbook(); java.io.ByteArrayOutputStream out = new java.io.ByteArrayOutputStream()) {
      Sheet sheet = workbook.createSheet("软压板基准清单");
      Row header = sheet.createRow(0);
      String[] headers = {"序号", "软压板名称", "软压板引用", "是否比对", "基准状态"};
      for (int i = 0; i < headers.length; i++) header.createCell(i).setCellValue(headers[i]);
      Row row = sheet.createRow(1);
      row.createCell(0).setCellValue(1);
      row.createCell(1).setCellValue("差动保护软压板");
      row.createCell(2).setCellValue("ref-1");
      row.createCell(3).setCellValue("是");
      row.createCell(4).setCellValue(state);
      workbook.write(out);
      return out.toByteArray();
    }
  }

  private ListResponse response(ItemResponse... items) {
    List<ItemResponse> list = Arrays.asList(items);
    return new ListResponse(SettingListScopeType.IED_DEVICE, 2L, "装置", 2L, "IED_A",
        SettingListScopeType.IED_DEVICE, 2L, false, list, list);
  }
}
