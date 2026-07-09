package com.zeta.business.logicnodecognition;

import com.zeta.business.auth.AuthService;
import com.zeta.business.user.UserRole;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import java.util.List;

@RestController
@Validated
public class LogicNodeCognitionController {

    private final LogicNodeCognitionService service;
    private final AuthService authService;

    public LogicNodeCognitionController(
            LogicNodeCognitionService service,
            AuthService authService) {
        this.service = service;
        this.authService = authService;
    }

    @GetMapping("/api/admin/logic-learning/logics/{logicDiagramId}/nodes")
    public List<LogicNodeSummaryResponse> listNodes(
            @RequestHeader(value = "Authorization", required = false) String authorization,
            @PathVariable Long logicDiagramId) {
        authService.requireRole(authorization, UserRole.ADMIN);
        return service.listConfigurableNodes(logicDiagramId);
    }

    @GetMapping("/api/admin/logic-learning/logics/{logicDiagramId}/node-items")
    public List<LogicNodeCognitionItemAdminResponse> listItems(
            @RequestHeader(value = "Authorization", required = false) String authorization,
            @PathVariable Long logicDiagramId,
            @RequestParam String nodeId) {
        authService.requireRole(authorization, UserRole.ADMIN);
        return service.listAdminItems(logicDiagramId, nodeId);
    }

    @PostMapping("/api/admin/logic-learning/logics/{logicDiagramId}/node-items")
    public LogicNodeCognitionItemAdminResponse create(
            @RequestHeader(value = "Authorization", required = false) String authorization,
            @PathVariable Long logicDiagramId,
            @RequestParam String nodeId,
            @Valid @RequestBody CreateLogicNodeCognitionItemRequest request) {
        authService.requireRole(authorization, UserRole.ADMIN);
        return service.create(logicDiagramId, nodeId, request);
    }

    @PutMapping("/api/admin/logic-learning/node-items/{id}")
    public LogicNodeCognitionItemAdminResponse update(
            @RequestHeader(value = "Authorization", required = false) String authorization,
            @PathVariable Long id,
            @Valid @RequestBody UpdateLogicNodeCognitionItemRequest request) {
        authService.requireRole(authorization, UserRole.ADMIN);
        return service.update(id, request);
    }

    @DeleteMapping("/api/admin/logic-learning/node-items/{id}")
    public void delete(
            @RequestHeader(value = "Authorization", required = false) String authorization,
            @PathVariable Long id) {
        authService.requireRole(authorization, UserRole.ADMIN);
        service.delete(id);
    }

    @GetMapping("/api/knowledge/protection-logics/{logicDiagramId}/node-items")
    public List<LogicNodeCognitionItemResponse> listLearnerItems(
            @RequestHeader(value = "Authorization", required = false) String authorization,
            @PathVariable Long logicDiagramId,
            @RequestParam String nodeId) {
        authService.requireUser(authorization);
        return service.listEnabledItems(logicDiagramId, nodeId);
    }
}
