package com.zeta.business.logiclearning;

import com.zeta.business.auth.AuthService;
import com.zeta.business.user.UserRole;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import java.util.Collections;
import java.util.Map;

@RestController
@Validated
@RequestMapping("/api/admin/logic-learning/logics")
public class LogicLearningConfigController {

    private final LogicLearningConfigService configService;
    private final AuthService authService;

    public LogicLearningConfigController(LogicLearningConfigService configService, AuthService authService) {
        this.configService = configService;
        this.authService = authService;
    }

    @PutMapping("/{logicDiagramId}/sort-order")
    public Map<String, Integer> updateSortOrder(
            @RequestHeader(value = "Authorization", required = false) String authorization,
            @PathVariable Long logicDiagramId,
            @Valid @RequestBody UpdateLogicLearningSortOrderRequest request) {
        authService.requireRole(authorization, UserRole.ADMIN);
        int sortOrder = configService.updateSortOrder(logicDiagramId, request.getSortOrder());
        return Collections.singletonMap("sortOrder", sortOrder);
    }
}
