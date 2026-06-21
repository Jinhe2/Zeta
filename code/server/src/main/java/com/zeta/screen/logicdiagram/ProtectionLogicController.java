package com.zeta.screen.logicdiagram;

import com.zeta.business.auth.AuthService;
import com.zeta.business.snapshot.LogicSnapshotService;
import com.zeta.business.snapshot.TriggerSnapshotResponse;
import com.zeta.business.user.User;
import com.zeta.screen.logicdiagram.ProtectionLogicService;
import com.zeta.screen.logicdiagram.ProtectionLogicDetailResponse;
import com.zeta.screen.logicdiagram.ProtectionLogicSummaryResponse;
import com.zeta.screen.logicdiagram.SectionSnapshotResponse;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/protection-logics")
public class ProtectionLogicController {

    private final ProtectionLogicService protectionLogicService;
    private final LogicSnapshotService logicSnapshotService;
    private final AuthService authService;

    public ProtectionLogicController(
            ProtectionLogicService protectionLogicService,
            LogicSnapshotService logicSnapshotService,
            AuthService authService) {
        this.protectionLogicService = protectionLogicService;
        this.logicSnapshotService = logicSnapshotService;
        this.authService = authService;
    }

    @GetMapping
    public List<ProtectionLogicSummaryResponse> list(
            @RequestHeader(value = "Authorization", required = false) String authorization) {
        authService.requireUser(authorization);
        return protectionLogicService.listSummaries();
    }

    @GetMapping("/{id}")
    public ProtectionLogicDetailResponse detail(
            @RequestHeader(value = "Authorization", required = false) String authorization,
            @PathVariable Long id) {
        authService.requireUser(authorization);
        return protectionLogicService.getDetail(id);
    }

    @GetMapping("/{id}/sections")
    public List<SectionSnapshotResponse> sections(
            @RequestHeader(value = "Authorization", required = false) String authorization,
            @PathVariable Long id) {
        authService.requireUser(authorization);
        return protectionLogicService.listSections(id);
    }

    /** 触发实验，生成断面数据 */
    @PostMapping("/{id}/snapshots")
    public TriggerSnapshotResponse triggerSnapshot(
            @RequestHeader("Authorization") String authorization,
            @PathVariable Long id) {
        User user = authService.requireUser(authorization);
        return logicSnapshotService.generateSnapshot(user, id);
    }

    /** 手动导入 JSON 断面数据 */
    @PostMapping("/{id}/snapshots/import")
    public TriggerSnapshotResponse importSnapshot(
            @RequestHeader("Authorization") String authorization,
            @PathVariable Long id,
            @RequestBody com.zeta.business.snapshot.ImportSnapshotRequest body) {
        User user = authService.requireUser(authorization);
        return logicSnapshotService.importSnapshot(user, id, body.getSnapshotJson());
    }
}
