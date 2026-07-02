package com.zeta.business.monitor;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.*;

/**
 * 查询 monitor_task 表（底层库，只读）。
 */
@Service
@Transactional(value = "businessTransactionManager", readOnly = true)
public class MonitorTaskService {

    private final MonitorTaskRepository repository;

    public MonitorTaskService(MonitorTaskRepository repository) {
        this.repository = repository;
    }

    public Map<String, Object> getTask(Long id) {
        MonitorTask task = repository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "监视任务不存在: " + id));
        return toResponse(task);
    }

    public List<Map<String, Object>> listByLogicDiagram(Long logicDiagramId) {
        List<Map<String, Object>> result = new ArrayList<>();
        for (MonitorTask task : repository.findByLogicDiagramIdOrderByCreatedAtDesc(logicDiagramId)) {
            result.add(toSummary(task));
        }
        return result;
    }

    private Map<String, Object> toResponse(MonitorTask task) {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("id", task.getId());
        map.put("uuid", task.getUuid());
        map.put("iedDeviceId", task.getIedDeviceId());
        map.put("logicDiagramId", task.getLogicDiagramId());
        map.put("state", task.getState() != null ? task.getState().name() : null);
        map.put("errorCode", task.getErrorCode());
        map.put("errorMessage", task.getErrorMessage());
        map.put("totalTransitions", task.getTotalTransitions());
        map.put("snapshotJson", task.getSnapshotJson());
        map.put("startedAt", task.getStartedAt());
        map.put("completedAt", task.getCompletedAt());
        map.put("createdAt", task.getCreatedAt());
        return map;
    }

    private Map<String, Object> toSummary(MonitorTask task) {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("id", task.getId());
        map.put("uuid", task.getUuid());
        map.put("state", task.getState() != null ? task.getState().name() : null);
        map.put("totalTransitions", task.getTotalTransitions());
        map.put("errorCode", task.getErrorCode());
        map.put("createdAt", task.getCreatedAt());
        map.put("completedAt", task.getCompletedAt());
        return map;
    }
}
