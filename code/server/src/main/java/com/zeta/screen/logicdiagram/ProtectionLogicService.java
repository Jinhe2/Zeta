package com.zeta.screen.logicdiagram;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import javax.annotation.PostConstruct;
import java.io.InputStream;
import java.util.*;

@Service
@Transactional(value = "screenTransactionManager", readOnly = true)
public class ProtectionLogicService {

    private static final Logger log = LoggerFactory.getLogger(ProtectionLogicService.class);

    private final ProtectionLogicRepository repository;
    private final ObjectMapper objectMapper;

    /** v2.3 snapshot data indexed by logicId from the snapshot JSON */
    private final Map<String, Map<String, Object>> snapshotCache = new LinkedHashMap<>();

    public ProtectionLogicService(ProtectionLogicRepository repository, ObjectMapper objectMapper) {
        this.repository = repository;
        this.objectMapper = objectMapper;
    }

    @PostConstruct
    void loadSnapshots() {
        try {
            PathMatchingResourcePatternResolver resolver = new PathMatchingResourcePatternResolver();
            Resource[] resources = resolver.getResources("classpath:snapshots/snapshot_*.json");
            for (Resource res : resources) {
                try (InputStream is = res.getInputStream()) {
                    Map<String, Object> snap = objectMapper.readValue(is, new TypeReference<Map<String, Object>>() {});
                    String logicId = String.valueOf(snap.getOrDefault("logicId", ""));
                    if (!logicId.isEmpty()) {
                        snapshotCache.put(logicId, snap);
                        log.info("Loaded v2.3 snapshot: {} ({} transitions)",
                                logicId, snap.getOrDefault("totalTransitions", "?"));
                    }
                } catch (Exception e) {
                    log.warn("Failed to load snapshot {}: {}", res.getFilename(), e.getMessage());
                }
            }
            log.info("Snapshot cache loaded: {} entries, logicIds: {}",
                    snapshotCache.size(), snapshotCache.keySet());
        } catch (Exception e) {
            log.warn("No snapshot files found in classpath:snapshots/: {}", e.getMessage());
        }
    }

    public List<ProtectionLogicSummaryResponse> listSummaries() {
        List<ProtectionLogicSummaryResponse> result = new ArrayList<>();
        for (ProtectionLogic logic : repository.findAllByOrderByIdAsc()) {
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

        // 1. Try v2.3 snapshot match by logicId
        Map<String, Object> snapshot = findSnapshot(logic.getLogicId(), logic.getProtectType());
        if (snapshot != null) {
            List<SectionSnapshotResponse> sections = buildSectionsFromSnapshot(snapshot);
            if (!sections.isEmpty()) {
                return sections;
            }
        }

        // 2. Try legacy config sections
        List<String> nodeIds = collectNodeIds(config);
        return buildSections(nodeIds, config);
    }

    /**
     * Find a matching snapshot by logicId or protectType keyword.
     */
    private Map<String, Object> findSnapshot(String logicId, String protectType) {
        // Exact match on logicId
        if (snapshotCache.containsKey(logicId)) {
            return snapshotCache.get(logicId);
        }
        // Keyword match on protectType or logicId
        String searchKey = (protectType != null ? protectType : "") + " " + (logicId != null ? logicId : "");
        searchKey = searchKey.toLowerCase();
        for (Map.Entry<String, Map<String, Object>> entry : snapshotCache.entrySet()) {
            String key = entry.getKey().toLowerCase();
            if (key.contains("differential") && (searchKey.contains("差动") || searchKey.contains("differential"))) {
                return entry.getValue();
            }
            if (key.contains("overcurrent") && (searchKey.contains("过流") || searchKey.contains("overcurrent"))) {
                return entry.getValue();
            }
            if (key.contains("reclose") && (searchKey.contains("重合闸") || searchKey.contains("reclose"))) {
                return entry.getValue();
            }
        }
        return null;
    }

    /**
     * Convert v2.3 snapshot format to SectionSnapshotResponse list.
     * Each timestamp becomes a section; channels[i].values[k] maps to nodes[i].id.
     */
    @SuppressWarnings("unchecked")
    private List<SectionSnapshotResponse> buildSectionsFromSnapshot(Map<String, Object> snapshot) {
        List<Map<String, Object>> nodes = (List<Map<String, Object>>) snapshot.get("nodes");
        List<String> timestamps = (List<String>) snapshot.get("timestamps");
        List<Map<String, Object>> channels = (List<Map<String, Object>>) snapshot.get("channels");

        if (nodes == null || timestamps == null || channels == null) {
            return Collections.emptyList();
        }

        List<SectionSnapshotResponse> sections = new ArrayList<>();
        String baseTimestamp = timestamps.get(0);
        long baseMs = parseTimestampMs(baseTimestamp);

        for (int k = 0; k < timestamps.size(); k++) {
            Map<String, Boolean> states = new LinkedHashMap<>();
            for (int i = 0; i < nodes.size() && i < channels.size(); i++) {
                String nodeId = String.valueOf(nodes.get(i).get("id"));
                Map<String, Object> channel = channels.get(i);
                List<Number> values = (List<Number>) channel.get("values");
                if (values != null && k < values.size()) {
                    states.put(nodeId, values.get(k).intValue() != 0);
                } else {
                    states.put(nodeId, false);
                }
            }

            String timestamp = timestamps.get(k);
            double elapsedSec = (parseTimestampMs(timestamp) - baseMs) / 1000.0;
            String label = String.format("T = %.3f s", elapsedSec);

            sections.add(new SectionSnapshotResponse(
                    "section-" + k,
                    label,
                    elapsedSec,
                    timestamp,
                    states));
        }
        return sections;
    }

    /**
     * Format a timestamp into a relative time label: "T = X.XXX s"
     */
    private String formatTimestampLabel(String timestamp, String baseTimestamp) {
        try {
            long baseMs = parseTimestampMs(baseTimestamp);
            long currentMs = parseTimestampMs(timestamp);
            double elapsedSec = (currentMs - baseMs) / 1000.0;
            return String.format("T = %.3f s", elapsedSec);
        } catch (Exception e) {
            return timestamp;
        }
    }

    /**
     * Parse "YYYY/MM/DD HH:MM:SS.mmm" to milliseconds since epoch (approximate).
     */
    private long parseTimestampMs(String ts) {
        // Extract time part: HH:MM:SS.mmm
        int spaceIdx = ts.indexOf(' ');
        String timePart = spaceIdx >= 0 ? ts.substring(spaceIdx + 1) : ts;
        String[] parts = timePart.split("[:.]");
        long h = Long.parseLong(parts[0]);
        long m = Long.parseLong(parts[1]);
        long s = Long.parseLong(parts[2]);
        long ms = parts.length > 3 ? Long.parseLong(parts[3]) : 0;
        return ((h * 60 + m) * 60 + s) * 1000 + ms;
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
                        null,
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
                    null,
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
