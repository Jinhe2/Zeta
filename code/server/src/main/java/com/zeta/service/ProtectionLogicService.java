package com.zeta.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.zeta.domain.ProtectionLogic;
import com.zeta.repository.ProtectionLogicRepository;
import com.zeta.web.dto.ProtectionLogicDetailResponse;
import com.zeta.web.dto.ProtectionLogicSummaryResponse;
import com.zeta.web.dto.SectionSnapshotResponse;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.*;

@Service
public class ProtectionLogicService {

    private final ProtectionLogicRepository repository;
    private final ObjectMapper objectMapper;

    public ProtectionLogicService(ProtectionLogicRepository repository, ObjectMapper objectMapper) {
        this.repository = repository;
        this.objectMapper = objectMapper;
    }

    public List<ProtectionLogicSummaryResponse> listSummaries() {
        List<ProtectionLogicSummaryResponse> result = new ArrayList<>();
        for (ProtectionLogic logic : repository.findByEnabledTrueOrderBySortOrderAscIdAsc()) {
            result.add(toSummary(logic));
        }
        return result;
    }

    public ProtectionLogicDetailResponse getDetail(Long id) {
        ProtectionLogic logic = requireLogic(id);
        return toDetail(logic);
    }

    public List<SectionSnapshotResponse> listSections(Long id) {
        ProtectionLogic logic = requireLogic(id);
        Map<String, Object> config = parseConfig(logic.getConfigJson());
        List<String> nodeIds = collectNodeIds(config);
        return buildSections(nodeIds, config);
    }

    private ProtectionLogic requireLogic(Long id) {
        return repository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "保护逻辑不存在"));
    }

    private ProtectionLogicSummaryResponse toSummary(ProtectionLogic logic) {
        Map<String, Object> config = parseConfig(logic.getConfigJson());
        return new ProtectionLogicSummaryResponse(
                logic.getId(),
                logic.getCode(),
                logic.getTitle(),
                logic.getDescription(),
                logic.getCategory(),
                listSize(config.get("inputs")),
                listSize(config.get("gates")),
                listSize(config.get("outputs")));
    }

    private ProtectionLogicDetailResponse toDetail(ProtectionLogic logic) {
        return new ProtectionLogicDetailResponse(
                logic.getId(),
                logic.getCode(),
                logic.getTitle(),
                logic.getDescription(),
                logic.getCategory(),
                parseConfig(logic.getConfigJson()));
    }

    @SuppressWarnings("unchecked")
    private int listSize(Object value) {
        if (value instanceof List) {
            return ((List<?>) value).size();
        }
        return 0;
    }

    @SuppressWarnings("unchecked")
    private List<String> collectNodeIds(Map<String, Object> config) {
        List<String> ids = new ArrayList<>();
        appendIds(config.get("inputs"), ids, "id");
        appendIds(config.get("gates"), ids, "id");
        appendIds(config.get("timers"), ids, "id");
        appendIds(config.get("outputs"), ids, "id");
        return ids;
    }

    @SuppressWarnings("unchecked")
    private void appendIds(Object listValue, List<String> ids, String key) {
        if (!(listValue instanceof List)) {
            return;
        }
        for (Object item : (List<?>) listValue) {
            if (item instanceof Map) {
                Object id = ((Map<String, Object>) item).get(key);
                if (id != null) {
                    ids.add(String.valueOf(id));
                }
            }
        }
    }

    @SuppressWarnings("unchecked")
    private List<SectionSnapshotResponse> buildSections(List<String> nodeIds, Map<String, Object> config) {
        Object sectionsObj = config.get("sections");
        if (sectionsObj instanceof List && !((List<?>) sectionsObj).isEmpty()) {
            List<SectionSnapshotResponse> sections = new ArrayList<>();
            List<?> rawList = (List<?>) sectionsObj;
            for (int i = 0; i < rawList.size(); i++) {
                Object raw = rawList.get(i);
                if (!(raw instanceof Map)) {
                    continue;
                }
                Map<String, Object> item = (Map<String, Object>) raw;
                Map<String, Boolean> states = new LinkedHashMap<>();
                Object statesObj = item.get("states");
                if (statesObj == null) {
                    statesObj = item.get("nodeStates");
                }
                if (statesObj instanceof Map) {
                    for (String nodeId : nodeIds) {
                        Object v = ((Map<String, Object>) statesObj).get(nodeId);
                        states.put(nodeId, parseBool(v));
                    }
                }
                double time = item.get("time") instanceof Number
                        ? ((Number) item.get("time")).doubleValue()
                        : i * 0.5;
                sections.add(new SectionSnapshotResponse(
                        String.valueOf(item.getOrDefault("id", "section-" + i)),
                        String.valueOf(item.getOrDefault("label", "T = " + time + " s")),
                        time,
                        states));
            }
            return sections;
        }

        Map<String, String> displayState = config.get("displayState") instanceof Map
                ? (Map<String, String>) config.get("displayState")
                : Collections.emptyMap();
        double[] times = {0, 0.2, 0.5, 1.0, 2.0, 5.0};
        List<SectionSnapshotResponse> sections = new ArrayList<>();
        for (int i = 0; i < times.length; i++) {
            Map<String, Boolean> states = new LinkedHashMap<>();
            for (String nodeId : nodeIds) {
                if (i == 0 && displayState.containsKey(nodeId)) {
                    states.put(nodeId, parseBool(displayState.get(nodeId)));
                } else {
                    states.put(nodeId, (hash(nodeId + "-" + i) % 100) < (28 + i * 12));
                }
            }
            sections.add(new SectionSnapshotResponse(
                    "section-" + i,
                    String.format("T = %.1f s", times[i]),
                    times[i],
                    states));
        }
        return sections;
    }

    private boolean parseBool(Object value) {
        if (value instanceof Boolean) {
            return (Boolean) value;
        }
        String s = String.valueOf(value).trim().toLowerCase();
        if ("1".equals(s) || "true".equals(s) || "yes".equals(s)) {
            return true;
        }
        try {
            return Double.parseDouble(s) > 0;
        } catch (NumberFormatException e) {
            return false;
        }
    }

    private int hash(String s) {
        int h = 0;
        for (int i = 0; i < s.length(); i++) {
            h = h * 31 + s.charAt(i);
        }
        return Math.abs(h);
    }

    private Map<String, Object> parseConfig(String json) {
        try {
            return objectMapper.readValue(json, new TypeReference<Map<String, Object>>() {});
        } catch (Exception e) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "配置解析失败");
        }
    }
}
