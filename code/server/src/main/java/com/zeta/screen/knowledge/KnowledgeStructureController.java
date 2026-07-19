package com.zeta.screen.knowledge;

import com.zeta.business.auth.AuthService;
import com.zeta.business.cabinetdisplay.CabinetDisplayItemResponse;
import com.zeta.business.cabinetdisplay.CabinetDisplayItemService;
import com.zeta.business.cognitiondevice.CognitionDeviceResponse;
import com.zeta.business.cognitiondevice.CognitionDeviceService;
import com.zeta.business.devicedisplay.DeviceDisplayItemResponse;
import com.zeta.business.devicedisplay.DeviceDisplayItemService;
import com.zeta.screen.baseline.IedBaselineSettingResponse;
import com.zeta.screen.baseline.IedBaselineSettingService;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/knowledge")
public class KnowledgeStructureController {

    private final KnowledgeStructureService knowledgeStructureService;
    private final DeviceDisplayItemService deviceDisplayItemService;
    private final CognitionDeviceService cognitionDeviceService;
    private final CabinetDisplayItemService cabinetDisplayItemService;
    private final IedBaselineSettingService iedBaselineSettingService;
    private final AuthService authService;

    public KnowledgeStructureController(
            KnowledgeStructureService knowledgeStructureService,
            DeviceDisplayItemService deviceDisplayItemService,
            CognitionDeviceService cognitionDeviceService,
            CabinetDisplayItemService cabinetDisplayItemService,
            IedBaselineSettingService iedBaselineSettingService,
            AuthService authService) {
        this.knowledgeStructureService = knowledgeStructureService;
        this.deviceDisplayItemService = deviceDisplayItemService;
        this.cognitionDeviceService = cognitionDeviceService;
        this.cabinetDisplayItemService = cabinetDisplayItemService;
        this.iedBaselineSettingService = iedBaselineSettingService;
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

    @GetMapping("/cabinets/{id}/display-items")
    public List<CabinetDisplayItemResponse> listCabinetDisplayItems(
            @RequestHeader(value = "Authorization", required = false) String authorization,
            @PathVariable Long id) {
        authService.requireUser(authorization);
        return cabinetDisplayItemService.listEnabledByScreenCabinet(id);
    }

    @GetMapping("/cabinet-display-items/{itemId}/cognition-devices")
    public List<CognitionDeviceResponse> listCognitionDevices(
            @RequestHeader(value = "Authorization", required = false) String authorization,
            @PathVariable Long itemId) {
        authService.requireUser(authorization);
        return cognitionDeviceService.listEnabledByCabinetDisplayItem(itemId);
    }

    @GetMapping("/cognition-devices/{id}/display-items")
    public List<DeviceDisplayItemResponse> listCognitionDeviceDisplayItems(
            @RequestHeader(value = "Authorization", required = false) String authorization,
            @PathVariable Long id) {
        authService.requireUser(authorization);
        return deviceDisplayItemService.listEnabledByCognitionDevice(id);
    }

    @GetMapping("/cognition-devices/{id}/baseline-settings")
    public List<IedBaselineSettingResponse> listIedBaselineSettings(
            @RequestHeader(value = "Authorization", required = false) String authorization,
            @PathVariable Long id) {
        authService.requireUser(authorization);
        return iedBaselineSettingService.listForCognitionDevice(id);
    }
}
