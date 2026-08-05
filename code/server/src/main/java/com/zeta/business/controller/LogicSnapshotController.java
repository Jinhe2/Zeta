package com.zeta.business.controller;

import com.zeta.business.auth.*;
import com.zeta.business.auth.AuthService;
import com.zeta.business.auth.dto.*;
import com.zeta.business.entities.binding.*;
import com.zeta.business.entities.binding.dto.*;
import com.zeta.business.entities.cabinetdisplay.*;
import com.zeta.business.entities.cabinetdisplay.dto.*;
import com.zeta.business.entities.cognitiondevice.*;
import com.zeta.business.entities.cognitiondevice.dto.*;
import com.zeta.business.entities.devicedisplay.*;
import com.zeta.business.entities.devicedisplay.dto.*;
import com.zeta.business.entities.drawinglearning.*;
import com.zeta.business.entities.drawinglearning.dto.*;
import com.zeta.business.entities.learningresource.*;
import com.zeta.business.entities.learningresource.dto.*;
import com.zeta.business.entities.logiclearning.*;
import com.zeta.business.entities.logiclearning.dto.*;
import com.zeta.business.entities.logicnodecognition.*;
import com.zeta.business.entities.logicnodecognition.dto.*;
import com.zeta.business.entities.monitor.*;
import com.zeta.business.entities.snapshot.*;
import com.zeta.business.entities.snapshot.dto.*;
import com.zeta.business.entities.user.*;
import com.zeta.business.entities.user.User;
import com.zeta.business.entities.user.dto.*;
import com.zeta.business.media.*;
import com.zeta.business.service.*;
import com.zeta.business.storage.*;
import com.zeta.screen.logicdiagram.SectionSnapshotResponse;
import java.util.List;
import org.springframework.web.bind.annotation.*;

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
      @RequestHeader("Authorization") String authorization, @RequestParam Long logicId) {
    User user = authService.requireUser(authorization);
    return snapshotService.listUserSnapshotsByLogic(user, logicId);
  }

  /** 断面详情（含原始 JSON） */
  @GetMapping("/{id}")
  public SnapshotDetailResponse detail(
      @RequestHeader("Authorization") String authorization, @PathVariable Long id) {
    User user = authService.requireUser(authorization);
    LogicSnapshot s = snapshotService.requireSnapshot(user, id);
    return new SnapshotDetailResponse(
        s.getId(),
        s.getLogicId(),
        s.getLogicCode(),
        s.getLogicName(),
        s.getTotalTransitions(),
        s.getStatus(),
        s.getErrorMessage(),
        s.getCreatedAt(),
        s.getCompletedAt(),
        s.getSnapshotJson());
  }

  /** 断面 sections 数据 */
  @GetMapping("/{id}/sections")
  public List<SectionSnapshotResponse> sections(
      @RequestHeader("Authorization") String authorization, @PathVariable Long id) {
    User user = authService.requireUser(authorization);
    LogicSnapshot snapshot = snapshotService.requireSnapshot(user, id);
    return snapshotService.parseSections(snapshot.getSnapshotJson());
  }
}
