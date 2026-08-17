package com.zeta.business.controller;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;

import com.zeta.business.auth.AuthService;
import com.zeta.business.entities.settinglist.SettingListScopeType;
import com.zeta.business.entities.user.UserRole;
import com.zeta.business.service.*;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

class SoftPressboardListControllerTest {
  @Test
  void 管理接口拒绝非管理员并且不访问清单() {
    AuthService authService = mock(AuthService.class);
    SoftPressboardListService listService = mock(SoftPressboardListService.class);
    when(authService.requireRole("Bearer student", UserRole.ADMIN))
        .thenThrow(new ResponseStatusException(HttpStatus.FORBIDDEN, "权限不足"));
    SoftPressboardListController controller = new SoftPressboardListController(
        authService, listService, mock(SoftPressboardComparisonService.class),
        mock(SoftPressboardListExcelService.class));
    assertThrows(ResponseStatusException.class,
        () -> controller.get("Bearer student", SettingListScopeType.IED_DEVICE, 1L));
    verifyNoInteractions(listService);
  }
}
