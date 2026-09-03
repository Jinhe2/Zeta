package com.zeta.business.controller;

import com.zeta.business.auth.AuthService;
import com.zeta.business.entities.user.User;
import com.zeta.business.entities.wholeexperiment.*;
import com.zeta.business.entities.wholeexperiment.WholeExperimentDtos.*;
import com.zeta.business.entities.experiment.ExperimentPrecheckResponse;
import com.zeta.business.service.*;
import com.zeta.integration.monitor.MonitorCommandService;
import java.util.*;
import org.junit.jupiter.api.*;
import org.springframework.web.server.ResponseStatusException;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class WholeExperimentControllerTest {
  AuthService auth = mock(AuthService.class);
  WholeExperimentService experiments = mock(WholeExperimentService.class);
  WholeExperimentMergeService merge = mock(WholeExperimentMergeService.class);
  WholeExperimentRunService runs = mock(WholeExperimentRunService.class);
  MonitorCommandService monitor = mock(MonitorCommandService.class);
  WholeExperimentController controller = new WholeExperimentController(auth, experiments, merge, runs, monitor);

  @BeforeEach void 登录() {
    User user = new User(); user.setId(1L);
    when(auth.requireUser("测试身份")).thenReturn(user);
  }

  @Test void 启动接口不允许绕过基准校验() {
    List<Member> members = Arrays.asList(new Member(11L, "甲", "甲", 1), new Member(22L, "乙", "乙", 2));
    when(experiments.validatedMembers(1L, 7L)).thenReturn(members);
    when(merge.check(Arrays.asList(11L, 22L))).thenReturn(new ExperimentPrecheckResponse("MISMATCH", null, null, null, null));
    assertThrows(ResponseStatusException.class, () -> controller.monitor("测试身份", 7L, new MonitorRequest()));
    verifyNoInteractions(runs, monitor);
  }

  @Test void 不同组合不能控制同一任务() {
    WholeExperimentRun run = new WholeExperimentRun(); run.setExperimentId(99L);
    when(runs.require(1L, "任务")).thenReturn(run);
    MonitorRequest request = new MonitorRequest(); request.setAction("end"); request.setTaskUuid("任务");
    assertThrows(ResponseStatusException.class, () -> controller.monitor("测试身份", 7L, request));
    verifyNoInteractions(monitor);
  }

  @Test void 历史入口只读取不隐式启动设备() {
    controller.detail("测试身份", 7L);
    controller.list("测试身份", 7L);
    verify(experiments).detail(1L, 7L);
    verify(runs).list(1L, 7L);
    verifyNoInteractions(monitor, merge);
  }
}
