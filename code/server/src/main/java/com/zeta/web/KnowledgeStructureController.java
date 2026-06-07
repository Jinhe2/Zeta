package com.zeta.web;

import com.zeta.service.AuthService;
import com.zeta.service.DeviceCognitionItemService;
import com.zeta.service.KnowledgeStructureService;
import com.zeta.web.dto.*;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/knowledge")
public class KnowledgeStructureController {

    private final KnowledgeStructureService knowledgeStructureService;
    private final DeviceCognitionItemService cognitionItemService;
    private final AuthService authService;

    public KnowledgeStructureController(
            KnowledgeStructureService knowledgeStructureService,
            DeviceCognitionItemService cognitionItemService,
            AuthService authService) {
        this.knowledgeStructureService = knowledgeStructureService;
        this.cognitionItemService = cognitionItemService;
        this.authService = authService;
    }

    @GetMapping("/tree")
    public KnowledgeTreeResponse tree(
            @RequestHeader(value = "Authorization", required = false) String authorization) {
        authService.requireUser(authorization);
        return knowledgeStructureService.getKnowledgeTree();
    }

    @GetMapping("/cabinets")
    public List<CabinetSummaryResponse> listCabinets(
            @RequestHeader(value = "Authorization", required = false) String authorization) {
        authService.requireUser(authorization);
        return knowledgeStructureService.listCabinets();
    }

    @GetMapping("/cabinets/{id}")
    public CabinetDetailResponse cabinetDetail(
            @RequestHeader(value = "Authorization", required = false) String authorization,
            @PathVariable Long id) {
        authService.requireUser(authorization);
        return knowledgeStructureService.getCabinet(id);
    }

    @GetMapping("/cabinets/{id}/devices")
    public List<DeviceSummaryResponse> listDevices(
            @RequestHeader(value = "Authorization", required = false) String authorization,
            @PathVariable Long id) {
        authService.requireUser(authorization);
        return knowledgeStructureService.listDevicesByCabinet(id);
    }

    @GetMapping("/devices/{id}")
    public DeviceDetailResponse deviceDetail(
            @RequestHeader(value = "Authorization", required = false) String authorization,
            @PathVariable Long id) {
        authService.requireUser(authorization);
        return knowledgeStructureService.getDevice(id);
    }

    @GetMapping("/devices/{id}/protection-logics")
    public List<ProtectionLogicBriefResponse> listProtectionLogics(
            @RequestHeader(value = "Authorization", required = false) String authorization,
            @PathVariable Long id) {
        authService.requireUser(authorization);
        return knowledgeStructureService.listProtectionLogicsByDevice(id);
    }

    @GetMapping("/devices/{id}/cognition-items")
    public List<DeviceCognitionItemResponse> listCognitionItems(
            @RequestHeader(value = "Authorization", required = false) String authorization,
            @PathVariable Long id) {
        authService.requireUser(authorization);
        return cognitionItemService.listEnabledByDevice(id);
    }
}
