package com.zeta.business.snapshot;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.zeta.business.user.User;
import com.zeta.screen.logicdiagram.ProtectionLogic;
import com.zeta.screen.logicdiagram.ProtectionLogicRepository;
import com.zeta.screen.logicdiagram.SectionSnapshotResponse;
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
import java.time.Instant;
import java.util.*;
import java.util.concurrent.ThreadLocalRandom;

@Service
public class LogicSnapshotService {

    private static final Logger log = LoggerFactory.getLogger(LogicSnapshotService.class);

    private final LogicSnapshotRepository snapshotRepository;
    private final ProtectionLogicRepository protectionLogicRepository;
    private final ObjectMapper objectMapper;

    /** v2.3 snapshot templates indexed by logicId */
    private final Map<String, String> templateCache = new LinkedHashMap<>();

    public LogicSnapshotService(LogicSnapshotRepository snapshotRepository,
                                ProtectionLogicRepository protectionLogicRepository,
                                ObjectMapper objectMapper) {
        this.snapshotRepository = snapshotRepository;
        this.protectionLogicRepository = protectionLogicRepository;
        this.objectMapper = objectMapper;
    }

    @PostConstruct
    void loadTemplates() {
        try {
            PathMatchingResourcePatternResolver resolver = new PathMatchingResourcePatternResolver();
            Resource[] resources = resolver.getResources("classpath:snapshots/snapshot_*.json");
            for (Resource res : resources) {
                try (InputStream is = res.getInputStream()) {
                    byte[] bytes = new byte[is.available()];
                    is.read(bytes);
                    String json = new String(bytes);
                    Map<String, Object> snap = objectMapper.readValue(json, new TypeReference<Map<String, Object>>() {});
                    String logicId = String.valueOf(snap.getOrDefault("logicId", ""));
                    if (!logicId.isEmpty()) {
                        templateCache.put(logicId, json);
                        log.info("Loaded snapshot template: {} ({} bytes)", logicId, json.length());
                    }
                } catch (Exception e) {
                    log.warn("Failed to load snapshot template {}: {}", res.getFilename(), e.getMessage());
                }
            }
            log.info("Snapshot template cache: {} entries", templateCache.size());
        } catch (Exception e) {
            log.warn("No snapshot templates found: {}", e.getMessage());
        }
    }

    // ── Generate ──────────────────────────────────────────────────────────

    @Transactional("businessTransactionManager")
    public TriggerSnapshotResponse generateSnapshot(User user, Long protectionLogicId) {
        ProtectionLogic logic = protectionLogicRepository.findById(protectionLogicId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "保护逻辑不存在"));

        // Find matching template
        String templateJson = findTemplate(logic.getLogicId(), logic.getProtectType());
        if (templateJson == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "未找到匹配的断面模板");
        }

        // Apply random perturbation to simulate real experiment
        String perturbedJson = perturbSnapshot(templateJson);

        // Parse total_transitions
        int totalTransitions = 0;
        try {
            Map<String, Object> snap = objectMapper.readValue(perturbedJson, new TypeReference<Map<String, Object>>() {});
            totalTransitions = snap.get("totalTransitions") instanceof Number
                    ? ((Number) snap.get("totalTransitions")).intValue() : 0;
        } catch (Exception ignored) {}

        LogicSnapshot snapshot = new LogicSnapshot();
        snapshot.setUserId(user.getId());
        snapshot.setLogicId(protectionLogicId);
        snapshot.setLogicCode(logic.getLogicId());
        snapshot.setLogicName(logic.getLogicName());
        snapshot.setSnapshotJson(perturbedJson);
        snapshot.setTotalTransitions(totalTransitions);
        snapshot.setStatus("COMPLETED");
        snapshot.setCreatedAt(Instant.now());
        snapshot.setCompletedAt(Instant.now());

        snapshotRepository.save(snapshot);
        log.info("Generated snapshot id={} user={} logic={}", snapshot.getId(), user.getUsername(), logic.getLogicId());

        return new TriggerSnapshotResponse(
                snapshot.getId(),
                "COMPLETED",
                totalTransitions,
                "断面数据已生成");
    }

    // ── Import (manual JSON) ──────────────────────────────────────────────

    @Transactional("businessTransactionManager")
    public TriggerSnapshotResponse importSnapshot(User user, Long protectionLogicId, String rawJson) {
        ProtectionLogic logic = protectionLogicRepository.findById(protectionLogicId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "保护逻辑不存在"));

        // Validate JSON structure
        Map<String, Object> parsed;
        try {
            parsed = objectMapper.readValue(rawJson, new TypeReference<Map<String, Object>>() {});
        } catch (Exception e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "JSON 格式错误: " + e.getMessage());
        }

        // Validate required v2.3 fields
        List<String> missing = new ArrayList<>();
        if (!parsed.containsKey("nodes")) missing.add("nodes");
        if (!parsed.containsKey("channels")) missing.add("channels");
        if (!parsed.containsKey("timestamps")) missing.add("timestamps");
        if (!missing.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "缺少 v2.3 必要字段: " + String.join(", ", missing));
        }

        // Validate arrays
        if (!(parsed.get("nodes") instanceof List) || !(parsed.get("channels") instanceof List)
                || !(parsed.get("timestamps") instanceof List)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "nodes/channels/timestamps 必须为数组");
        }

        @SuppressWarnings("unchecked")
        int nodeCount = ((List<?>) parsed.get("nodes")).size();
        @SuppressWarnings("unchecked")
        int channelCount = ((List<?>) parsed.get("channels")).size();
        @SuppressWarnings("unchecked")
        int tsCount = ((List<?>) parsed.get("timestamps")).size();

        if (nodeCount != channelCount) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "nodes(" + nodeCount + ") 与 channels(" + channelCount + ") 数量不一致");
        }

        int totalTransitions = parsed.get("totalTransitions") instanceof Number
                ? ((Number) parsed.get("totalTransitions")).intValue()
                : Math.max(0, tsCount - 1);

        // Re-serialize to ensure clean JSON
        String cleanJson;
        try {
            cleanJson = objectMapper.writeValueAsString(parsed);
        } catch (Exception e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "JSON 序列化失败");
        }

        LogicSnapshot snapshot = new LogicSnapshot();
        snapshot.setUserId(user.getId());
        snapshot.setLogicId(protectionLogicId);
        snapshot.setLogicCode(logic.getLogicId());
        snapshot.setLogicName(logic.getLogicName());
        snapshot.setSnapshotJson(cleanJson);
        snapshot.setTotalTransitions(totalTransitions);
        snapshot.setStatus("COMPLETED");
        snapshot.setSource("MANUAL");
        snapshot.setCreatedAt(Instant.now());
        snapshot.setCompletedAt(Instant.now());

        snapshotRepository.save(snapshot);
        log.info("Imported manual snapshot id={} user={} logic={}", snapshot.getId(), user.getUsername(), logic.getLogicId());

        return new TriggerSnapshotResponse(
                snapshot.getId(),
                "COMPLETED",
                totalTransitions,
                "断面数据已导入（手动）");
    }

    // ── Query ─────────────────────────────────────────────────────────────

    public List<SnapshotSummaryResponse> listUserSnapshots(User user) {
        List<SnapshotSummaryResponse> result = new ArrayList<>();
        for (LogicSnapshot s : snapshotRepository.findByUserIdOrderByCreatedAtDesc(user.getId())) {
            result.add(toSummary(s));
        }
        return result;
    }

    public List<SnapshotSummaryResponse> listUserSnapshotsByLogic(User user, Long logicId) {
        List<SnapshotSummaryResponse> result = new ArrayList<>();
        for (LogicSnapshot s : snapshotRepository.findByUserIdAndLogicIdOrderByCreatedAtDesc(user.getId(), logicId)) {
            result.add(toSummary(s));
        }
        return result;
    }

    public LogicSnapshot requireSnapshot(User user, Long snapshotId) {
        return snapshotRepository.findByIdAndUserId(snapshotId, user.getId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "断面数据不存在"));
    }

    // ── Parse v2.3 → Sections ─────────────────────────────────────────────

    @SuppressWarnings("unchecked")
    public List<SectionSnapshotResponse> parseSections(String snapshotJson) {
        try {
            Map<String, Object> snapshot = objectMapper.readValue(snapshotJson, new TypeReference<Map<String, Object>>() {});
            List<Map<String, Object>> nodes = (List<Map<String, Object>>) snapshot.get("nodes");
            List<String> timestamps = (List<String>) snapshot.get("timestamps");
            List<Map<String, Object>> channels = (List<Map<String, Object>>) snapshot.get("channels");

            if (nodes == null || timestamps == null || channels == null) {
                return Collections.emptyList();
            }

            String baseTimestamp = timestamps.get(0);
            long baseMs = parseTimestampMs(baseTimestamp);

            List<SectionSnapshotResponse> sections = new ArrayList<>();
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
        } catch (Exception e) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "断面数据解析失败: " + e.getMessage());
        }
    }

    // ── Internals ─────────────────────────────────────────────────────────

    private String findTemplate(String logicId, String protectType) {
        // Exact match
        if (templateCache.containsKey(logicId)) {
            return templateCache.get(logicId);
        }
        // Keyword match
        String searchKey = ((protectType != null ? protectType : "") + " " + (logicId != null ? logicId : "")).toLowerCase();
        for (Map.Entry<String, String> entry : templateCache.entrySet()) {
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
     * Apply minor random perturbation to a v2.3 snapshot template.
     * Flips a few input channel values to simulate experiment variation.
     */
    @SuppressWarnings("unchecked")
    private String perturbSnapshot(String templateJson) {
        try {
            Map<String, Object> snap = objectMapper.readValue(templateJson, new TypeReference<Map<String, Object>>() {});
            List<Map<String, Object>> nodes = (List<Map<String, Object>>) snap.get("nodes");
            List<Map<String, Object>> channels = (List<Map<String, Object>>) snap.get("channels");
            List<String> timestamps = (List<String>) snap.get("timestamps");

            if (nodes == null || channels == null || timestamps == null) {
                return templateJson;
            }

            ThreadLocalRandom rng = ThreadLocalRandom.current();

            // Perturb 1-3 input channels: flip a random value
            int perturbCount = rng.nextInt(1, 4);
            for (int p = 0; p < perturbCount; p++) {
                for (int i = 0; i < nodes.size() && i < channels.size(); i++) {
                    String type = String.valueOf(nodes.get(i).getOrDefault("type", ""));
                    if (!"input".equals(type)) continue;
                    if (rng.nextBoolean()) continue; // 50% skip

                    Map<String, Object> channel = channels.get(i);
                    List<Integer> values = new ArrayList<>((List<Integer>) channel.get("values"));
                    int flipIdx = rng.nextInt(values.size());
                    values.set(flipIdx, values.get(flipIdx) == 0 ? 1 : 0);
                    channel.put("values", values);
                    break;
                }
            }

            // Update fileId with new UUID
            String origFileId = String.valueOf(snap.getOrDefault("fileId", "SNAPSHOT"));
            snap.put("fileId", origFileId.substring(0, origFileId.lastIndexOf('_') + 1) + UUID.randomUUID());

            return objectMapper.writeValueAsString(snap);
        } catch (Exception e) {
            log.warn("Perturbation failed, using original template: {}", e.getMessage());
            return templateJson;
        }
    }

    private SnapshotSummaryResponse toSummary(LogicSnapshot s) {
        return new SnapshotSummaryResponse(
                s.getId(),
                s.getLogicId(),
                s.getLogicCode(),
                s.getLogicName(),
                s.getTotalTransitions(),
                s.getStatus(),
                s.getSource(),
                s.getCreatedAt(),
                s.getCompletedAt());
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
}
