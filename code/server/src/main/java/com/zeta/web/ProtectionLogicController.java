package com.zeta.web;

import com.zeta.service.AuthService;
import com.zeta.service.ProtectionLogicService;
import com.zeta.web.dto.ProtectionLogicDetailResponse;
import com.zeta.web.dto.ProtectionLogicSummaryResponse;
import com.zeta.web.dto.SectionSnapshotResponse;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/protection-logics")
public class ProtectionLogicController {

    private final ProtectionLogicService protectionLogicService;
    private final AuthService authService;

    public ProtectionLogicController(
            ProtectionLogicService protectionLogicService,
            AuthService authService) {
        this.protectionLogicService = protectionLogicService;
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
}
