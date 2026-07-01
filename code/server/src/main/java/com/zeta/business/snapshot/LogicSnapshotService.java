package com.zeta.business.snapshot;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.zeta.business.user.User;
import com.zeta.screen.logicdiagram.ProtectionLogic;
import com.zeta.screen.logicdiagram.ProtectionLogicRepository;
import com.zeta.screen.logicdiagram.SectionSnapshotResponse;
import com.zeta.screen.logicdiagram.dto.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.ThreadLocalRandom;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Service
public class LogicSnapshotService {

    private static final Logger log = LoggerFactory.getLogger(LogicSnapshotService.class);
    private static final DateTimeFormatter TS_FMT = DateTimeFormatter
            .ofPattern("yyyy/MM/dd HH:mm:ss.SSS")
            .withZone(ZoneId.systemDefault());

    private final LogicSnapshotRepository snapshotRepository;
    private final ProtectionLogicRepository protectionLogicRepository;
    private final ObjectMapper objectMapper;

    public LogicSnapshotService(LogicSnapshotRepository snapshotRepository,
                                ProtectionLogicRepository protectionLogicRepository,
                                ObjectMapper objectMapper) {
        this.snapshotRepository = snapshotRepository;
        this.protectionLogicRepository = protectionLogicRepository;
        this.objectMapper = objectMapper;
    }

    // ── Generate（从 config_json 生成断面数据）──────────────────────────────

    @Transactional("businessTransactionManager")
    public TriggerSnapshotResponse generateSnapshot(User user, Long protectionLogicId) {
        ProtectionLogic logic = protectionLogicRepository.findById(protectionLogicId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "保护逻辑不存在"));

        ConfigDto config = parseConfig(logic.getConfigJson());
        List<NodeInfo> nodeInfos = collectNodeInfos(config);
        if (nodeInfos.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "配置中无有效节点");
        }

        String snapshotJson = generateV23Snapshot(nodeInfos, config.getDisplayState());

        int totalTransitions = 0;
        try {
            Map<String, Object> snap = objectMapper.readValue(snapshotJson, new TypeReference<Map<String, Object>>() {});
            totalTransitions = snap.get("totalTransitions") instanceof Number
                    ? ((Number) snap.get("totalTransitions")).intValue() : 0;
        } catch (Exception ignored) {}

        LogicSnapshot snapshot = new LogicSnapshot();
        snapshot.setUserId(user.getId());
        snapshot.setLogicId(protectionLogicId);
        snapshot.setLogicCode(logic.getLogicId());
        snapshot.setLogicName(logic.getLogicName());
        snapshot.setSnapshotJson(snapshotJson);
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

        Map<String, Object> parsed;
        try {
            parsed = objectMapper.readValue(rawJson, new TypeReference<Map<String, Object>>() {});
        } catch (Exception e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "JSON 格式错误: " + e.getMessage());
        }

        List<String> missing = new ArrayList<>();
        if (!parsed.containsKey("nodes")) missing.add("nodes");
        if (!parsed.containsKey("channels")) missing.add("channels");
        if (!parsed.containsKey("timestamps")) missing.add("timestamps");
        if (!missing.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "缺少 v2.3 必要字段: " + String.join(", ", missing));
        }

        if (!(parsed.get("nodes") instanceof List) || !(parsed.get("channels") instanceof List)
                || !(parsed.get("timestamps") instanceof List)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "nodes/channels/timestamps 必须为数组");
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

    /**
     * 从 config_json 收集所有节点信息（id + type）。
     */
    private List<NodeInfo> collectNodeInfos(ConfigDto config) {
        List<NodeInfo> infos = new ArrayList<>();
        Set<String> seen = new HashSet<>();

        for (InputDto input : nullSafe(config.getInputs())) {
            if (seen.add(input.getId())) {
                infos.add(new NodeInfo(input.getId(), "input"));
            }
        }
        for (GateDto gate : nullSafe(config.getGates())) {
            if (seen.add(gate.getId())) {
                infos.add(new NodeInfo(gate.getId(), "gate"));
            }
        }
        for (TimerDto timer : nullSafe(config.getTimers())) {
            if (seen.add(timer.getId())) {
                infos.add(new NodeInfo(timer.getId(), "timer"));
            }
        }
        for (OutputDto output : nullSafe(config.getOutputs())) {
            if (seen.add(output.getId())) {
                infos.add(new NodeInfo(output.getId(), "output"));
            }
        }
        return infos;
    }

    /**
     * 从节点信息生成 v2.3 格式断面 JSON。
     * 模拟实验过程：初始状态部分满足 → 逐步变化 → 最终动作。
     */
    private String generateV23Snapshot(List<NodeInfo> nodeInfos, Map<String, String> displayState) {
        ThreadLocalRandom rng = ThreadLocalRandom.current();
        Instant now = Instant.now();

        // 生成 8 个时间点（20ms 间隔，模拟保护动作过程）
        int tsCount = 8;
        List<String> timestamps = new ArrayList<>();
        for (int k = 0; k < tsCount; k++) {
            timestamps.add(TS_FMT.format(now.plusMillis(k * 20L)));
        }

        // 为每个节点生成通道值序列
        List<Map<String, Object>> nodes = new ArrayList<>();
        List<Map<String, Object>> channels = new ArrayList<>();
        int totalTransitions = 0;

        for (NodeInfo node : nodeInfos) {
            Map<String, Object> nodeEntry = new LinkedHashMap<>();
            nodeEntry.put("id", node.id);
            nodeEntry.put("type", node.type);
            nodes.add(nodeEntry);

            List<Integer> values = new ArrayList<>();

            // 初始值：优先取 displayState，否则 input=1 其余=0
            int initVal;
            if (displayState != null && displayState.containsKey(node.id)) {
                initVal = parseBool(displayState.get(node.id)) ? 1 : 0;
            } else {
                initVal = "input".equals(node.type) ? 1 : 0;
            }

            // 生成变化序列：模拟信号逐步传播
            for (int k = 0; k < tsCount; k++) {
                int val;
                if (k == 0) {
                    val = initVal;
                } else {
                    // 随时间推移，gate/timer/output 逐步变为 1（模拟保护动作传播）
                    int activateProbability = 20 + k * 12; // 每个时间步增加激活概率
                    if ("input".equals(node.type)) {
                        // input 信号偶尔翻转
                        val = initVal;
                        if (rng.nextInt(100) < 8) {
                            val = 1 - val;
                        }
                    } else if ("output".equals(node.type)) {
                        // output 在最后几个时间步更可能动作
                        val = (k >= tsCount - 2 && rng.nextInt(100) < 60) ? 1 : 0;
                    } else {
                        // gate/timer 随时间逐步激活
                        val = rng.nextInt(100) < activateProbability ? 1 : 0;
                    }
                }
                values.add(val);
            }

            // 统计变位次数
            for (int k = 1; k < values.size(); k++) {
                if (!values.get(k).equals(values.get(k - 1))) {
                    totalTransitions++;
                }
            }

            Map<String, Object> channel = new LinkedHashMap<>();
            channel.put("values", values);
            channels.add(channel);
        }

        // 组装 v2.3 格式
        Map<String, Object> snapshot = new LinkedHashMap<>();
        snapshot.put("fileId", "SNAPSHOT_" + UUID.randomUUID());
        snapshot.put("logicId", "generated");
        snapshot.put("totalTransitions", totalTransitions);
        snapshot.put("nodes", nodes);
        snapshot.put("timestamps", timestamps);
        snapshot.put("channels", channels);

        try {
            return objectMapper.writeValueAsString(snapshot);
        } catch (Exception e) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "断面 JSON 生成失败");
        }
    }

    private ConfigDto parseConfig(String json) {
        try {
            return objectMapper.readValue(json, ConfigDto.class);
        } catch (Exception e) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "配置解析失败");
        }
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

    private static <T> List<T> nullSafe(List<T> list) {
        return list != null ? list : Collections.emptyList();
    }

    /** 节点信息（id + 类型） */
    private static class NodeInfo {
        final String id;
        final String type;

        NodeInfo(String id, String type) {
            this.id = id;
            this.type = type;
        }
    }
}
