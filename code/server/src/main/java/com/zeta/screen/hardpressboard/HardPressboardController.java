package com.zeta.screen.hardpressboard;

import com.zeta.business.auth.AuthService;
import org.springframework.web.bind.annotation.*;

import java.util.*;
import java.util.stream.Collectors;

/** 硬压板只读查询（底层库数据） */
@RestController
@RequestMapping("/api/hard-pressboards")
public class HardPressboardController {

    private final HardPressboardRepository repository;
    private final AuthService authService;

    public HardPressboardController(HardPressboardRepository repository, AuthService authService) {
        this.repository = repository;
        this.authService = authService;
    }

    @GetMapping(params = "cabinetId")
    public List<Map<String, Object>> listByCabinet(
            @RequestHeader(value = "Authorization", required = false) String authorization,
            @RequestParam Long cabinetId) {
        authService.requireUser(authorization);

        return repository.findByCabinetIdOrderByIdAsc(cabinetId).stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    private Map<String, Object> toResponse(HardPressboard pb) {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("id", pb.getId());
        map.put("cabinetId", pb.getCabinet() != null ? pb.getCabinet().getId() : null);
        map.put("name", pb.getName());
        map.put("pressboardType", pb.getPressboardType() != null ? pb.getPressboardType().name() : null);
        map.put("rowNo", pb.getRowNo());
        map.put("colNo", pb.getColNo());
        map.put("iedDeviceId", pb.getIedDevice() != null ? pb.getIedDevice().getId() : null);
        map.put("iedSignalRef", pb.getIedSignalRef());
        map.put("inputChannelNo", pb.getInputChannelNo());
        map.put("outputChannelNo", pb.getOutputChannelNo());
        map.put("description", pb.getDescription());
        return map;
    }
}
