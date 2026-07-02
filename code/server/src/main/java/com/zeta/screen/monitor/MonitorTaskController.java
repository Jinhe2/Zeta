package com.zeta.screen.monitor;

import com.zeta.business.auth.AuthService;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * 实验监视任务查询（底层库只读）。
 */
@RestController
@RequestMapping("/api/monitor/tasks")
public class MonitorTaskController {

    private final MonitorTaskService taskService;
    private final AuthService authService;

    public MonitorTaskController(MonitorTaskService taskService, AuthService authService) {
        this.taskService = taskService;
        this.authService = authService;
    }

    /** 按 ID 查询任务详情（含 snapshot_json） */
    @GetMapping("/{id}")
    public Map<String, Object> getTask(
            @RequestHeader(value = "Authorization", required = false) String authorization,
            @PathVariable Long id) {
        authService.requireUser(authorization);
        return taskService.getTask(id);
    }

    /** 按逻辑框图 ID 查询任务列表 */
    @GetMapping(params = "logicDiagramId")
    public List<Map<String, Object>> listByLogicDiagram(
            @RequestHeader(value = "Authorization", required = false) String authorization,
            @RequestParam Long logicDiagramId) {
        authService.requireUser(authorization);
        return taskService.listByLogicDiagram(logicDiagramId);
    }
}
