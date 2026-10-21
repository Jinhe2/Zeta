package com.zeta.business.controller;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.zeta.business.auth.AuthService;
import com.zeta.business.entities.pressboardchange.PressboardChange;
import com.zeta.business.entities.pressboardchange.PressboardChangeRepository;
import java.util.Arrays;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

class PressboardChangeControllerTest {

    @Test
    void 压板变位实体和仓库明确归属业务持久化单元() {
        assertEquals("com.zeta.business.entities.pressboardchange",
                PressboardChange.class.getPackage().getName());
        assertEquals("com.zeta.business.entities.pressboardchange",
                PressboardChangeRepository.class.getPackage().getName());
    }

    @Test
    void 返回仓库的倒序记录并映射公开字段() {
        PressboardChangeRepository repository = mock(PressboardChangeRepository.class);
        AuthService authService = mock(AuthService.class);
        PressboardChangeController controller =
                new PressboardChangeController(repository, authService);

        PressboardChange newest = change(12L, 8L, "功能压板一", "ON", 2000L);
        PressboardChange older = change(11L, 9L, "功能压板二", "OFF", 1000L);
        when(repository.findByCabinetIdOrderByChangeTimeMsDescIdDesc(3L))
                .thenReturn(Arrays.asList(newest, older));

        List<PressboardChangeController.Response> result = controller.list("Bearer token", 3L);

        verify(authService).requireUser("Bearer token");
        assertEquals(2, result.size());
        assertEquals(12L, result.get(0).getId());
        assertEquals(8L, result.get(0).getPressboardId());
        assertEquals("功能压板一", result.get(0).getPressboardName());
        assertEquals("ON", result.get(0).getState());
        assertEquals(2000L, result.get(0).getChangeTimeMs());
        assertEquals(11L, result.get(1).getId());
    }

    @Test
    void 未登录时拒绝查询() {
        PressboardChangeRepository repository = mock(PressboardChangeRepository.class);
        AuthService authService = mock(AuthService.class);
        PressboardChangeController controller = new PressboardChangeController(
                repository, authService);
        doThrow(new ResponseStatusException(HttpStatus.UNAUTHORIZED, "未登录"))
                .when(authService).requireUser(null);

        ResponseStatusException error = assertThrows(
                ResponseStatusException.class,
                () -> controller.list(null, 3L));

        assertEquals(HttpStatus.UNAUTHORIZED, error.getStatus());
    }

    private PressboardChange change(
            Long id,
            Long pressboardId,
            String name,
            String state,
            Long time) {
        PressboardChange change = new PressboardChange();
        change.setId(id);
        change.setCabinetId(3L);
        change.setPressboardId(pressboardId);
        change.setPressboardName(name);
        change.setState(state);
        change.setChangeTimeMs(time);
        return change;
    }
}
