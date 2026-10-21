package com.zeta.business.controller;

import com.zeta.business.auth.AuthService;
import com.zeta.business.entities.pressboardchange.PressboardChange;
import com.zeta.business.entities.pressboardchange.PressboardChangeRepository;
import java.util.List;
import java.util.stream.Collectors;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** 当前屏柜的功能硬压板变位历史查询接口。 */
@RestController
@RequestMapping("/api/pressboard-changes")
public class PressboardChangeController {

    private final PressboardChangeRepository repository;
    private final AuthService authService;

    public PressboardChangeController(
            PressboardChangeRepository repository,
            AuthService authService) {
        this.repository = repository;
        this.authService = authService;
    }

    @GetMapping("/{cabinetId}")
    @Transactional(value = "businessTransactionManager", readOnly = true)
    public List<Response> list(
            @RequestHeader(value = "Authorization", required = false) String authorization,
            @PathVariable Long cabinetId) {
        authService.requireUser(authorization);
        return repository.findByCabinetIdOrderByChangeTimeMsDescIdDesc(cabinetId).stream()
                .map(Response::new)
                .collect(Collectors.toList());
    }

    public static class Response {
        private final Long id;
        private final Long pressboardId;
        private final String pressboardName;
        private final String state;
        private final Long changeTimeMs;

        Response(PressboardChange change) {
            this.id = change.getId();
            this.pressboardId = change.getPressboardId();
            this.pressboardName = change.getPressboardName();
            this.state = change.getState();
            this.changeTimeMs = change.getChangeTimeMs();
        }

        public Long getId() { return id; }
        public Long getPressboardId() { return pressboardId; }
        public String getPressboardName() { return pressboardName; }
        public String getState() { return state; }
        public Long getChangeTimeMs() { return changeTimeMs; }
    }
}
