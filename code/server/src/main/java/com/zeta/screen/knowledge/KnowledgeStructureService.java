package com.zeta.screen.knowledge;

import com.zeta.screen.cabinet.Cabinet;
import com.zeta.screen.ieddevice.Device;
import com.zeta.screen.logicdiagram.ProtectionLogic;
import com.zeta.screen.cabinet.CabinetRepository;
import com.zeta.screen.ieddevice.DeviceRepository;
import com.zeta.screen.logicdiagram.ProtectionLogicRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
@Transactional(value = "screenTransactionManager", readOnly = true)
public class KnowledgeStructureService {

    private final CabinetRepository cabinetRepository;
    private final DeviceRepository deviceRepository;
    private final ProtectionLogicRepository protectionLogicRepository;

    public KnowledgeStructureService(
            CabinetRepository cabinetRepository,
            DeviceRepository deviceRepository,
            ProtectionLogicRepository protectionLogicRepository) {
        this.cabinetRepository = cabinetRepository;
        this.deviceRepository = deviceRepository;
        this.protectionLogicRepository = protectionLogicRepository;
    }

    public List<CabinetSummaryResponse> listCabinets() {
        List<CabinetSummaryResponse> result = new ArrayList<>();
        for (Cabinet cabinet : cabinetRepository.findAllByOrderByIdAsc()) {
            int deviceCount = deviceRepository.findByCabinetIdOrderByIdAsc(cabinet.getId()).size();
            result.add(new CabinetSummaryResponse(
                    cabinet.getId(),
                    cabinet.getCode(),
                    cabinet.getName(),
                    cabinet.getDescription(),
                    0,
                    deviceCount));
        }
        return result;
    }

    public CabinetDetailResponse getCabinet(Long id) {
        Cabinet cabinet = requireCabinet(id);
        List<DeviceSummaryResponse> devices = listDevicesByCabinet(cabinet.getId());
        return new CabinetDetailResponse(
                cabinet.getId(),
                cabinet.getCode(),
                cabinet.getName(),
                cabinet.getDescription(),
                0,
                devices);
    }

    public List<DeviceSummaryResponse> listDevicesByCabinet(Long cabinetId) {
        requireCabinet(cabinetId);
        List<DeviceSummaryResponse> result = new ArrayList<>();
        for (Device device : deviceRepository.findByCabinetIdOrderByIdAsc(cabinetId)) {
            result.add(toDeviceSummary(device));
        }
        return result;
    }

    public DeviceDetailResponse getDevice(Long id) {
        Device device = requireDevice(id);
        List<ProtectionLogicBriefResponse> logics = listProtectionLogicsByDevice(device.getId());
        Cabinet cabinet = device.getCabinet();
        return new DeviceDetailResponse(
                device.getId(),
                cabinet.getId(),
                cabinet.getCode(),
                cabinet.getName(),
                device.getCode(),
                device.getName(),
                device.getDescription(),
                0,
                logics);
    }

    public List<ProtectionLogicBriefResponse> listProtectionLogicsByDevice(Long deviceId) {
        requireDevice(deviceId);
        return protectionLogicRepository.findByDeviceIdOrderByIdAsc(deviceId).stream()
                .map(this::toProtectionLogicBrief)
                .collect(Collectors.toList());
    }

    public KnowledgeTreeResponse getKnowledgeTree() {
        List<CabinetTreeNodeResponse> cabinets = new ArrayList<>();
        for (Cabinet cabinet : cabinetRepository.findAllByOrderByIdAsc()) {
            List<DeviceTreeNodeResponse> devices = new ArrayList<>();
            for (Device device : deviceRepository.findByCabinetIdOrderByIdAsc(cabinet.getId())) {
                List<ProtectionLogicBriefResponse> logics = protectionLogicRepository
                        .findByDeviceIdOrderByIdAsc(device.getId())
                        .stream()
                        .map(this::toProtectionLogicBrief)
                        .collect(Collectors.toList());
                devices.add(new DeviceTreeNodeResponse(
                        device.getId(),
                        device.getCode(),
                        device.getName(),
                        device.getDescription(),
                        0,
                        logics));
            }
            cabinets.add(new CabinetTreeNodeResponse(
                    cabinet.getId(),
                    cabinet.getCode(),
                    cabinet.getName(),
                    cabinet.getDescription(),
                    0,
                    devices));
        }
        return new KnowledgeTreeResponse(cabinets);
    }

    private DeviceSummaryResponse toDeviceSummary(Device device) {
        int logicCount = protectionLogicRepository.findByDeviceIdOrderByIdAsc(device.getId()).size();
        Cabinet cabinet = device.getCabinet();
        return new DeviceSummaryResponse(
                device.getId(),
                cabinet.getId(),
                cabinet.getName(),
                device.getCode(),
                device.getName(),
                device.getDescription(),
                0,
                logicCount);
    }

    private ProtectionLogicBriefResponse toProtectionLogicBrief(ProtectionLogic logic) {
        return new ProtectionLogicBriefResponse(
                logic.getId(),
                logic.getDevice().getId(),
                logic.getCode(),
                logic.getTitle(),
                logic.getDescription(),
                logic.getCategory(),
                0);
    }

    private Cabinet requireCabinet(Long id) {
        return cabinetRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "屏柜不存在"));
    }

    private Device requireDevice(Long id) {
        return deviceRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "设备不存在"));
    }
}
