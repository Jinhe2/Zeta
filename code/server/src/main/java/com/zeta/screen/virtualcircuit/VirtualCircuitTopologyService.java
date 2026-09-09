package com.zeta.screen.virtualcircuit;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.*;
import javax.sql.DataSource;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

/** 为教练模式提供 SCD 虚回路表的只读视图。 */
@Service
public class VirtualCircuitTopologyService {

  public static final String UNCONFIGURED_PORT = "未配置端口";

  private final NamedParameterJdbcTemplate jdbc;
  private final ObjectMapper objectMapper;

  public VirtualCircuitTopologyService(
      @Qualifier("screenDataSource") DataSource screenDataSource, ObjectMapper objectMapper) {
    this.jdbc = new NamedParameterJdbcTemplate(screenDataSource);
    this.objectMapper = objectMapper;
  }

  public List<Map<String, Object>> listEligibleDevices(Long cabinetId) {
    requireCabinet(cabinetId);
    String sql =
        "SELECT d.id, d.cabinet_id, d.ied_name, d.ied_desc "
            + "FROM ied_device d WHERE d.cabinet_id = :cabinetId AND EXISTS ("
            + "SELECT 1 FROM scd_virtual_control_block cb "
            + "WHERE BINARY cb.receiver_ied_name = BINARY d.ied_name "
            + "OR BINARY cb.source_ied_name = BINARY d.ied_name) ORDER BY d.id";
    return jdbc.query(
        sql,
        Collections.singletonMap("cabinetId", cabinetId),
        (rs, rowNum) -> deviceSummary(
            rs.getLong("id"),
            rs.getLong("cabinet_id"),
            rs.getString("ied_name"),
            rs.getString("ied_desc")));
  }

  public Map<String, Object> getTopology(Long cabinetId, Long iedDeviceId) {
    Map<String, Object> center = loadCenter(cabinetId, iedDeviceId);
    String centerName = String.valueOf(center.get("iedName"));
    Map<String, String> virtualDescriptions = new LinkedHashMap<>();

    String blockSql =
        "SELECT cb.id, cb.identity_key, cb.receiver_ied_name, cb.receiver_ied_desc, "
            + "cb.sort_order, cb.service_type, cb.source_ied_name, cb.source_ied_desc, "
            + "cb.source_ld_inst, cb.control_name, cb.control_description, "
            + "cb.control_reference, cb.dataset_name, cb.dataset_reference, cb.status, cb.error_message, "
            + "COALESCE(cb.manual_source_ports_json, cb.scd_source_ports_json) source_ports_json, "
            + "COALESCE(cb.manual_target_ports_json, cb.scd_target_ports_json) target_ports_json, "
            + "COALESCE(cb.manual_disconnect_signals_json, cb.scd_disconnect_signals_json) disconnect_json, "
            + "COALESCE(cb.manual_link_abnormal_signal_json, cb.scd_link_abnormal_signal_json) abnormal_json "
            + "FROM scd_virtual_control_block cb "
            + "WHERE BINARY cb.receiver_ied_name = BINARY :iedName "
            + "OR BINARY cb.source_ied_name = BINARY :iedName "
            + "ORDER BY cb.sort_order, cb.id";
    List<Map<String, Object>> blocks = jdbc.query(
        blockSql,
        Collections.singletonMap("iedName", centerName),
        (rs, rowNum) -> {
          Map<String, Object> block = new LinkedHashMap<>();
          block.put("id", rs.getLong("id"));
          block.put("identityKey", rs.getString("identity_key"));
          String receiverIedName = rs.getString("receiver_ied_name");
          block.put("receiverIedName", receiverIedName);
          rememberDescription(
              virtualDescriptions, receiverIedName, rs.getString("receiver_ied_desc"));
          block.put("sortOrder", rs.getInt("sort_order"));
          block.put("serviceType", rs.getString("service_type"));
          String sourceIedName = rs.getString("source_ied_name");
          rememberDescription(
              virtualDescriptions, sourceIedName, rs.getString("source_ied_desc"));
          block.put("sourceIedName", sourceIedName == null || sourceIedName.trim().isEmpty()
              ? "未知发送装置" : sourceIedName);
          block.put("sourceLdInst", rs.getString("source_ld_inst"));
          block.put("controlName", rs.getString("control_name"));
          block.put("controlDescription", rs.getString("control_description"));
          block.put("controlReference", rs.getString("control_reference"));
          block.put("datasetName", rs.getString("dataset_name"));
          block.put("datasetReference", rs.getString("dataset_reference"));
          block.put("status", rs.getString("status"));
          block.put("errorMessage", rs.getString("error_message"));
          block.put("sourcePorts", parseStringArray(rs.getString("source_ports_json")));
          block.put("targetPorts", parseStringArray(rs.getString("target_ports_json")));
          block.put("disconnectSignals", parseObjectArray(rs.getString("disconnect_json")));
          block.put("linkAbnormalSignals", parseObjectArray(rs.getString("abnormal_json")));
          block.put("connections", new ArrayList<Map<String, Object>>());
          return block;
        });

    if (blocks.isEmpty()) {
      throw new ResponseStatusException(HttpStatus.NOT_FOUND, "该装置没有虚回路信息");
    }

    Map<Long, Map<String, Object>> blocksById = new LinkedHashMap<>();
    List<Long> blockIds = new ArrayList<>();
    Set<String> iedNames = new LinkedHashSet<>();
    Set<String> receiverNames = new LinkedHashSet<>();
    iedNames.add(centerName);
    for (Map<String, Object> block : blocks) {
      Long id = ((Number) block.get("id")).longValue();
      blocksById.put(id, block);
      blockIds.add(id);
      String sourceName = nullableString(block.get("sourceIedName"));
      String receiverName = nullableString(block.get("receiverIedName"));
      if (!sourceName.isEmpty()) iedNames.add(sourceName);
      if (!receiverName.isEmpty()) {
        iedNames.add(receiverName);
        receiverNames.add(receiverName);
      }
    }
    loadConnections(blockIds, blocksById);

    Map<String, List<Map<String, Object>>> devicesByName = loadDevicesByName(iedNames);
    Map<String, List<Map<String, Object>>> portsByIed = loadPorts(iedNames);
    List<Map<String, Object>> peers = new ArrayList<>();
    for (String name : iedNames) {
      if (name.equals(centerName)) continue;
      Map<String, Object> peer = new LinkedHashMap<>();
      peer.put("iedName", name);
      List<Map<String, Object>> candidates = devicesByName.getOrDefault(name, Collections.emptyList());
      peer.put("displayName", displayName(name, candidates, virtualDescriptions.get(name)));
      peer.put("ports", portsByIed.getOrDefault(name, Collections.emptyList()));
      peers.add(peer);
    }

    List<Map<String, Object>> edges = aggregateEdges(blocks);
    List<Map<String, Object>> statusTargets = new ArrayList<>();
    for (String receiverName : receiverNames) {
      List<Map<String, Object>> candidates = devicesByName.getOrDefault(receiverName, Collections.emptyList());
      Map<String, Object> target = new LinkedHashMap<>();
      target.put("iedName", receiverName);
      boolean selectedCenter = receiverName.equals(centerName);
      target.put("available", selectedCenter || candidates.size() == 1);
      if (selectedCenter) {
        target.put("iedDeviceId", center.get("id"));
        target.put("cabinetId", center.get("cabinetId"));
        target.put("reason", null);
      } else if (candidates.size() == 1) {
        target.put("iedDeviceId", candidates.get(0).get("id"));
        target.put("cabinetId", candidates.get(0).get("cabinetId"));
        target.put("reason", null);
      } else {
        target.put("iedDeviceId", null);
        target.put("cabinetId", null);
        target.put("reason", candidates.isEmpty() ? "IED_NOT_CONFIGURED" : "IED_NAME_AMBIGUOUS");
      }
      statusTargets.add(target);
    }

    center.put("displayName", displayName(
        centerName, Collections.singletonList(center), virtualDescriptions.get(centerName)));
    center.put("ports", portsByIed.getOrDefault(centerName, Collections.emptyList()));
    Map<String, Object> result = new LinkedHashMap<>();
    result.put("center", center);
    result.put("peers", peers);
    result.put("edges", edges);
    result.put("controlBlocks", blocks);
    result.put("statusTargets", statusTargets);
    return result;
  }

  private void requireCabinet(Long cabinetId) {
    if (cabinetId == null || cabinetId <= 0) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "屏柜 ID 无效");
    }
    Integer count = jdbc.queryForObject(
        "SELECT COUNT(*) FROM cabinet WHERE id = :id",
        Collections.singletonMap("id", cabinetId), Integer.class);
    if (count == null || count == 0) {
      throw new ResponseStatusException(HttpStatus.NOT_FOUND, "屏柜不存在");
    }
  }

  private Map<String, Object> loadCenter(Long cabinetId, Long iedDeviceId) {
    requireCabinet(cabinetId);
    if (iedDeviceId == null || iedDeviceId <= 0) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "IED 装置 ID 无效");
    }
    List<Map<String, Object>> rows = jdbc.query(
        "SELECT id, cabinet_id, ied_name, ied_desc FROM ied_device WHERE id = :id",
        Collections.singletonMap("id", iedDeviceId),
        (rs, rowNum) -> deviceSummary(
            rs.getLong("id"), rs.getLong("cabinet_id"),
            rs.getString("ied_name"), rs.getString("ied_desc")));
    if (rows.isEmpty()) {
      throw new ResponseStatusException(HttpStatus.NOT_FOUND, "IED 装置不存在");
    }
    Map<String, Object> center = rows.get(0);
    if (!cabinetId.equals(center.get("cabinetId"))) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "装置不属于指定屏柜");
    }
    return center;
  }

  private Map<String, Object> deviceSummary(long id, long cabinetId, String iedName, String desc) {
    Map<String, Object> result = new LinkedHashMap<>();
    result.put("id", id);
    result.put("cabinetId", cabinetId);
    result.put("iedName", iedName);
    result.put("displayName", desc == null || desc.trim().isEmpty() ? iedName : desc);
    result.put("description", desc);
    return result;
  }

  @SuppressWarnings("unchecked")
  private List<String> parseStringArray(String json) {
    if (json == null || json.trim().isEmpty()) return Collections.emptyList();
    try {
      List<Object> raw = objectMapper.readValue(json, new TypeReference<List<Object>>() {});
      List<String> result = new ArrayList<>();
      for (Object value : raw) {
        if (value instanceof String && !((String) value).trim().isEmpty()) result.add((String) value);
      }
      return result;
    } catch (Exception ignored) {
      return Collections.emptyList();
    }
  }

  private List<Map<String, Object>> parseObjectArray(String json) {
    if (json == null || json.trim().isEmpty()) return Collections.emptyList();
    try {
      return objectMapper.readValue(json, new TypeReference<List<Map<String, Object>>>() {});
    } catch (Exception ignored) {
      return Collections.emptyList();
    }
  }

  @SuppressWarnings("unchecked")
  private void loadConnections(
      List<Long> ids, Map<Long, Map<String, Object>> blocksById) {
    String sql =
        "SELECT id, control_block_id, sort_order, source_description, source_reference, "
            + "target_description, target_reference, int_addr, fc, status, error_message "
            + "FROM scd_virtual_connection WHERE control_block_id IN (:ids) "
            + "ORDER BY control_block_id, sort_order, id";
    jdbc.query(sql, Collections.singletonMap("ids", ids), rs -> {
      Map<String, Object> item = new LinkedHashMap<>();
      item.put("id", rs.getLong("id"));
      item.put("sortOrder", rs.getInt("sort_order"));
      item.put("sourceDescription", rs.getString("source_description"));
      item.put("sourceReference", rs.getString("source_reference"));
      item.put("targetDescription", rs.getString("target_description"));
      item.put("targetReference", rs.getString("target_reference"));
      item.put("intAddr", rs.getString("int_addr"));
      item.put("fc", rs.getString("fc"));
      item.put("status", rs.getString("status"));
      item.put("errorMessage", rs.getString("error_message"));
      long blockId = rs.getLong("control_block_id");
      Map<String, Object> block = blocksById.get(blockId);
      if (block != null) ((List<Map<String, Object>>) block.get("connections")).add(item);
    });
  }

  private Map<String, List<Map<String, Object>>> loadDevicesByName(Set<String> names) {
    Map<String, List<Map<String, Object>>> result = new LinkedHashMap<>();
    String sql =
        "SELECT id, cabinet_id, ied_name, ied_desc FROM ied_device "
            + "WHERE BINARY ied_name IN (:names) ORDER BY id";
    jdbc.query(sql, Collections.singletonMap("names", names), rs -> {
      String name = rs.getString("ied_name");
      result.computeIfAbsent(name, key -> new ArrayList<>()).add(deviceSummary(
          rs.getLong("id"), rs.getLong("cabinet_id"), name, rs.getString("ied_desc")));
    });
    return result;
  }

  private Map<String, List<Map<String, Object>>> loadPorts(Set<String> names) {
    Map<String, List<Map<String, Object>>> result = new LinkedHashMap<>();
    String sql =
        "SELECT id, ied_name, sort_order, subnetwork_name, access_point_name, connection_type, "
            + "port_name, media_type, cable, plug FROM scd_ied_port "
            + "WHERE BINARY ied_name IN (:names) ORDER BY ied_name, sort_order, id";
    jdbc.query(sql, Collections.singletonMap("names", names), rs -> {
      Map<String, Object> port = new LinkedHashMap<>();
      port.put("id", rs.getLong("id"));
      port.put("sortOrder", rs.getInt("sort_order"));
      port.put("subnetworkName", rs.getString("subnetwork_name"));
      port.put("accessPointName", rs.getString("access_point_name"));
      port.put("connectionType", rs.getString("connection_type"));
      port.put("portName", rs.getString("port_name"));
      port.put("mediaType", rs.getString("media_type"));
      port.put("cable", rs.getString("cable"));
      port.put("plug", rs.getString("plug"));
      result.computeIfAbsent(rs.getString("ied_name"), key -> new ArrayList<>()).add(port);
    });
    return result;
  }

  @SuppressWarnings("unchecked")
  List<Map<String, Object>> aggregateEdges(List<Map<String, Object>> blocks) {
    Map<String, Map<String, Object>> grouped = new LinkedHashMap<>();
    for (Map<String, Object> block : blocks) {
      String source = nullableString(block.get("sourceIedName"));
      String target = nullableString(block.get("receiverIedName"));
      List<String> sourcePorts = (List<String>) block.get("sourcePorts");
      List<String> targetPorts = (List<String>) block.get("targetPorts");
      String sourcePort = sourcePorts.isEmpty() ? UNCONFIGURED_PORT : sourcePorts.get(0);
      String targetPort = targetPorts.isEmpty() ? UNCONFIGURED_PORT : targetPorts.get(0);
      String key = source + "\u0000" + sourcePort + "\u0000" + target + "\u0000" + targetPort;
      Map<String, Object> edge = grouped.get(key);
      if (edge == null) {
        edge = new LinkedHashMap<>();
        edge.put("id", "edge-" + grouped.size());
        edge.put("sourceIedName", source);
        edge.put("sourcePort", sourcePort);
        edge.put("targetIedName", target);
        edge.put("targetPort", targetPort);
        edge.put("controlBlockIds", new ArrayList<Long>());
        grouped.put(key, edge);
      }
      ((List<Long>) edge.get("controlBlockIds")).add(((Number) block.get("id")).longValue());
    }
    return new ArrayList<>(grouped.values());
  }

  String displayName(
      String fallback, List<Map<String, Object>> candidates, String virtualDescription) {
    if (candidates.size() == 1) {
      String configuredDescription = nullableString(candidates.get(0).get("description"));
      if (!configuredDescription.trim().isEmpty()) return configuredDescription;
    }
    if (virtualDescription != null && !virtualDescription.trim().isEmpty()) {
      return virtualDescription;
    }
    return fallback;
  }

  void rememberDescription(Map<String, String> descriptions, String iedName, String description) {
    if (iedName == null || iedName.trim().isEmpty()
        || description == null || description.trim().isEmpty()) return;
    descriptions.putIfAbsent(iedName, description);
  }

  private String nullableString(Object value) {
    return value == null ? "" : String.valueOf(value);
  }
}
