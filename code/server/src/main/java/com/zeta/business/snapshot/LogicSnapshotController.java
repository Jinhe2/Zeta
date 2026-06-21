package com.zeta.business.snapshot;

import com.zeta.business.auth.AuthService;
import com.zeta.business.user.User;
import com.zeta.screen.logicdiagram.SectionSnapshotResponse;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/snapshots")
public class LogicSnapshotController {

    private final LogicSnapshotService snapshotService;
    private final AuthService authService;

    public LogicSnapshotController(LogicSnapshotService snapshotService, AuthService authService) {
        this.snapshotService = snapshotService;
        this.authService = authService;
    }

    /** 学员断面列表 */
    @GetMapping
    public List<SnapshotSummaryResponse> listMine(
            @RequestHeader("Authorization") String authorization) {
        User user = authService.requireUser(authorization);
        return snapshotService.listUserSnapshots(user);
    }

    /** 某逻辑的断面列表 */
    @GetMapping(params = "logicId")
    public List<SnapshotSummaryResponse> listByLogic(
            @RequestHeader("Authorization") String authorization,
            @RequestParam Long logicId) {
        User user = authService.requireUser(authorization);
        return snapshotService.listUserSnapshotsByLogic(user, logicId);
    }

    /** 断面详情（含原始 JSON） */
    @GetMapping("/{id}")
    public SnapshotDetailResponse detail(
            @RequestHeader("Authorization") String authorization,
            @PathVariable Long id) {
        User user = authService.requireUser(authorization);
        LogicSnapshot s = snapshotService.requireSnapshot(user, id);
        return new SnapshotDetailResponse(
                s.getId(), s.getLogicId(), s.getLogicCode(), s.getLogicName(),
                s.getTotalTransitions(), s.getStatus(), s.getErrorMessage(),
                s.getCreatedAt(), s.getCompletedAt(), s.getSnapshotJson());
    }

    /** 断面 sections 数据 */
    @GetMapping("/{id}/sections")
    public List<SectionSnapshotResponse> sections(
            @RequestHeader("Authorization") String authorization,
            @PathVariable Long id) {
        User user = authService.requireUser(authorization);
        LogicSnapshot snapshot = snapshotService.requireSnapshot(user, id);
        return snapshotService.parseSections(snapshot.getSnapshotJson());
    }
}
