package com.zeta.business.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import com.zeta.business.entities.settinglist.SettingListScopeType;
import com.zeta.business.entities.settinglist.dto.*;
import java.util.*;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;

class SettingListExcelServiceTest {
  @Test
  void 导出文件可以原样导回并整套替换() {
    SettingListService listService = mock(SettingListService.class);
    SettingListExcelService excelService = new SettingListExcelService(listService);
    SettingListItemResponse item =
        new SettingListItemResponse("IED_A/LD0/PTOC$SG$StrVal", "SG", "启动值", "FLOAT", false, "1.5", 0);
    SettingListResponse response =
        new SettingListResponse(
            SettingListScopeType.IED_DEVICE,
            2L,
            "装置A",
            2L,
            "IED_A",
            SettingListScopeType.IED_DEVICE,
            2L,
            false,
            Collections.singletonList(item),
            Collections.singletonList(item));
    when(listService.get(SettingListScopeType.IED_DEVICE, 2L)).thenReturn(response);
    when(listService.replace(eq(SettingListScopeType.IED_DEVICE), eq(2L), anyList()))
        .thenReturn(response);

    byte[] workbook = excelService.exportWorkbook(SettingListScopeType.IED_DEVICE, 2L);
    SettingListResponse imported =
        excelService.importWorkbook(
            SettingListScopeType.IED_DEVICE,
            2L,
            new MockMultipartFile(
                "file",
                "定值清单.xlsx",
                "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
                workbook));

    assertSame(response, imported);
    verify(listService)
        .replace(
            eq(SettingListScopeType.IED_DEVICE),
            eq(2L),
            argThat(
                rows ->
                    rows.size() == 1
                        && "IED_A/LD0/PTOC$SG$StrVal".equals(rows.get(0).getSettingRef())
                        && "1.5".equals(rows.get(0).getBaselineValue())
                        && Boolean.FALSE.equals(rows.get(0).getCompareEnabled())));
  }
}
