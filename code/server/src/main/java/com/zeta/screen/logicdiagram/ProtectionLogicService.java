package com.zeta.screen.logicdiagram;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.zeta.screen.logicdiagram.dto.*;
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
import java.util.stream.Collectors;
import java.util.stream.Stream;

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
        ConfigDto config = parseConfig(logic.getConfigJson());

        // 1. Try v2.3 snapshot match by logicId
        Map<String, Object> snapshot = findSnapshot(logic.getLogicId(), logic.getProtectType());
        if (snapshot != null) {
            List<SectionSnapshotResponse> sections = buildSectionsFromSnapshot(snapshot);
            if (!sections.isEmpty()) {
                return sections;
            }
        }

        // 2. Try legacy config sections / synthetic fallback
        List<String> nodeIds = collectNodeIds(config);
        return buildSections(nodeIds, config);
    }

    /* ── 内部方法 ── */

    /**
     * Find a matching snapshot by logicId or protectType keyword.
     */
    private Map<String, Object> findSnapshot(String logicId, String protectType) {
        if (snapshotCache.containsKey(logicId)) {
            return snapshotCache.get(logicId);
        }
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
        long baseMs = parseTimestampMs(timestamps.get(0));

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
                    "section-" + k, label, elapsedSec, timestamp, states));
        }
        return sections;
    }

    private long parseTimestampMs(String ts) {
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
        ConfigDto config = parseConfig(logic.getConfigJson());
        return new ProtectionLogicSummaryResponse(
                logic.getId(),
                logic.getCode(),
                logic.getTitle(),
                logic.getDescription(),
                logic.getCategory(),
                sizeOf(config.getInputs()),
                sizeOf(config.getGates()),
                sizeOf(config.getOutputs()));
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

    private static <T> int sizeOf(List<T> list) {
        return list != null ? list.size() : 0;
    }

    private List<String> collectNodeIds(ConfigDto config) {
        return Stream.of(
                        nullSafe(config.getInputs()).stream().map(InputDto::getId),
                        nullSafe(config.getGates()).stream().map(GateDto::getId),
                        nullSafe(config.getTimers()).stream().map(TimerDto::getId),
                        nullSafe(config.getOutputs()).stream().map(OutputDto::getId))
                .flatMap(s -> s)
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
    }

    private List<SectionSnapshotResponse> buildSections(List<String> nodeIds, ConfigDto config) {
        // Tier 2: legacy config sections
        List<SectionDto> configSections = config.getSections();
        if (configSections != null && !configSections.isEmpty()) {
            List<SectionSnapshotResponse> sections = new ArrayList<>();
            for (int i = 0; i < configSections.size(); i++) {
                SectionDto sec = configSections.get(i);
                Map<String, Boolean> states = new LinkedHashMap<>();
                Map<String, Boolean> rawStates = sec.getStates() != null ? sec.getStates() : sec.getNodeStates();
                if (rawStates != null) {
                    for (String nodeId : nodeIds) {
                        states.put(nodeId, parseBool(rawStates.get(nodeId)));
                    }
                }
                double time = sec.getTime() != null ? sec.getTime() : i * 0.5;
                String id = sec.getId() != null ? sec.getId() : "section-" + i;
                String label = sec.getLabel() != null ? sec.getLabel() : "T = " + time + " s";
                sections.add(new SectionSnapshotResponse(id, label, time, null, states));
            }
            return sections;
        }

        // Tier 3: hash-based synthesis
        Map<String, String> displayState = config.getDisplayState() != null
                ? config.getDisplayState()
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
                    times[i], null, states));
        }
        return sections;
    }

    private boolean parseBool(Object value) {
        if (value == null) return false;
        if (value instanceof Boolean) return (Boolean) value;
        String s = String.valueOf(value).trim().toLowerCase();
        if ("1".equals(s) || "true".equals(s) || "yes".equals(s)) return true;
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

    private ConfigDto parseConfig(String json) {
        try {
            return objectMapper.readValue(json, ConfigDto.class);
        } catch (Exception e) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "配置解析失败");
        }
    }

    private static <T> List<T> nullSafe(List<T> list) {
        return list != null ? list : Collections.emptyList();
    }
}
