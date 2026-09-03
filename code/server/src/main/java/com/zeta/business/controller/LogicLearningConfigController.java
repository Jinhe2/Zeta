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
import com.zeta.business.entities.user.UserRole;
import com.zeta.business.entities.user.dto.*;
import com.zeta.business.media.*;
import com.zeta.business.service.*;
import com.zeta.business.storage.*;
import java.util.Collections;
import java.util.Map;
import javax.validation.Valid;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

@RestController
@Validated
@RequestMapping("/api/admin/logic-learning/logics")
public class LogicLearningConfigController {

  private final LogicLearningConfigService configService;
  private final AuthService authService;

  public LogicLearningConfigController(
      LogicLearningConfigService configService, AuthService authService) {
    this.configService = configService;
    this.authService = authService;
  }

  @PutMapping("/by-device/{deviceId}/configs")
  public java.util.List<UpdateLogicLearningConfigsRequest.Item> updateConfigs(
      @RequestHeader(value = "Authorization", required = false) String authorization,
      @PathVariable Long deviceId, @Valid @RequestBody UpdateLogicLearningConfigsRequest request) {
    authService.requireRole(authorization, UserRole.ADMIN);
    return configService.updateConfigs(deviceId, request.getItems());
  }

  @PutMapping("/{logicDiagramId}/whole-experiment-sequence")
  public Map<String, Integer> updateWholeExperimentSequence(
      @RequestHeader(value = "Authorization", required = false) String authorization,
      @PathVariable Long logicDiagramId,
      @Valid @RequestBody UpdateWholeExperimentSequenceRequest request) {
    authService.requireRole(authorization, UserRole.ADMIN);
    int sequence = configService.updateWholeExperimentSequence(logicDiagramId, request.getWholeExperimentSequence());
    return Collections.singletonMap("wholeExperimentSequence", sequence);
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
