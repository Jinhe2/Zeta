package com.zeta.screen.terminal;

import com.zeta.business.auth.AuthService;
import org.springframework.web.bind.annotation.*;

import java.util.*;
import java.util.stream.Collectors;

/** 端子/端子排只读查询（底层库数据） */
@RestController
@RequestMapping("/api/terminals")
public class TerminalController {

    private final TerminalRepository terminalRepository;
    private final TerminalStripRepository stripRepository;
    private final AuthService authService;

    public TerminalController(TerminalRepository terminalRepository,
                              TerminalStripRepository stripRepository,
                              AuthService authService) {
        this.terminalRepository = terminalRepository;
        this.stripRepository = stripRepository;
        this.authService = authService;
    }

    /** 查询屏柜下所有端子排 */
    @GetMapping("/strips")
    public List<Map<String, Object>> listStrips(
            @RequestHeader(value = "Authorization", required = false) String authorization,
            @RequestParam Long cabinetId) {
        authService.requireUser(authorization);

        return stripRepository.findByCabinetIdOrderBySortOrderAsc(cabinetId).stream()
                .map(this::stripToResponse)
                .collect(Collectors.toList());
    }

    /** 查询屏柜下所有端子 */
    @GetMapping(params = "cabinetId")
    public List<Map<String, Object>> listByCabinet(
            @RequestHeader(value = "Authorization", required = false) String authorization,
            @RequestParam Long cabinetId) {
        authService.requireUser(authorization);

        return terminalRepository.findByCabinetIdOrderByIdAsc(cabinetId).stream()
                .map(this::terminalToResponse)
                .collect(Collectors.toList());
    }

    /** 查询端子排下的端子 */
    @GetMapping(params = "stripId")
    public List<Map<String, Object>> listByStrip(
            @RequestHeader(value = "Authorization", required = false) String authorization,
            @RequestParam Long stripId) {
        authService.requireUser(authorization);

        return terminalRepository.findByTerminalStripIdOrderByIdAsc(stripId).stream()
                .map(this::terminalToResponse)
                .collect(Collectors.toList());
    }

    private Map<String, Object> stripToResponse(TerminalStrip strip) {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("id", strip.getId());
        map.put("cabinetId", strip.getCabinet() != null ? strip.getCabinet().getId() : null);
        map.put("name", strip.getName());
        map.put("labelPrefix", strip.getLabelPrefix());
        map.put("functionDesc", strip.getFunctionDesc());
        map.put("sortOrder", strip.getSortOrder());
        return map;
    }

    private Map<String, Object> terminalToResponse(Terminal t) {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("id", t.getId());
        map.put("cabinetId", t.getCabinet() != null ? t.getCabinet().getId() : null);
        map.put("terminalStripId", t.getTerminalStrip() != null ? t.getTerminalStrip().getId() : null);
        map.put("terminalLabel", t.getTerminalLabel());
        map.put("signalType", t.getSignalType() != null ? t.getSignalType().name() : null);
        map.put("iedDeviceId", t.getIedDevice() != null ? t.getIedDevice().getId() : null);
        map.put("iedSignalRef", t.getIedSignalRef());
        map.put("description", t.getDescription());
        return map;
    }
}
