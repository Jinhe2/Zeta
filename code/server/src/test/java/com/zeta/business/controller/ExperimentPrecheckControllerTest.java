package com.zeta.business.controller;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;

import com.zeta.business.auth.AuthService;
import com.zeta.business.service.ExperimentPrecheckService;
import java.util.Collections;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

class ExperimentPrecheckControllerTest {
  @Test
  void 未登录用户不能执行预检() {
    AuthService authService = mock(AuthService.class);
    ExperimentPrecheckService service = mock(ExperimentPrecheckService.class);
    when(authService.requireUser(null))
        .thenThrow(new ResponseStatusException(HttpStatus.UNAUTHORIZED, "未登录"));
    ExperimentPrecheckController controller = new ExperimentPrecheckController(authService, service);
    assertThrows(ResponseStatusException.class,
        () -> controller.check(null, Collections.<String, Object>singletonMap("logicDiagramId", 8)));
    verifyNoInteractions(service);
  }

  @Test
  void 缺少逻辑框图参数时拒绝请求() {
    AuthService authService = mock(AuthService.class);
    ExperimentPrecheckController controller = new ExperimentPrecheckController(
        authService, mock(ExperimentPrecheckService.class));
    assertThrows(ResponseStatusException.class,
        () -> controller.check("Bearer user", Collections.emptyMap()));
  }
}
