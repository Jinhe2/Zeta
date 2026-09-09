package com.zeta.screen.sampling;

import java.util.*;
import javax.sql.DataSource;
import lombok.Getter;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

/** 只读访问屏柜库中由配置工具维护的数字化采样关联。 */
@Service
public class SamplingSignalCatalogService {
  private final NamedParameterJdbcTemplate jdbc;

  public SamplingSignalCatalogService(@Qualifier("screenDataSource") DataSource dataSource) {
    this.jdbc = new NamedParameterJdbcTemplate(dataSource);
  }

  public List<Map<String, Object>> listDevices(Long cabinetId) {
    requireCabinet(cabinetId);
    return jdbc.query(
        "SELECT id, ied_name, ied_desc FROM ied_device WHERE cabinet_id=:cabinetId ORDER BY id",
        Collections.singletonMap("cabinetId", cabinetId),
        (rs, row) -> {
          Map<String, Object> item = new LinkedHashMap<>();
          item.put("id", rs.getLong("id"));
          item.put("iedName", rs.getString("ied_name"));
          String description = rs.getString("ied_desc");
          item.put("displayName", description == null || description.trim().isEmpty()
              ? rs.getString("ied_name") : description);
          return item;
        });
  }

  public Map<String, Object> listCandidates(Long cabinetId, Long iedDeviceId) {
    Map<String, Object> device = requireDevice(cabinetId, iedDeviceId);
    List<Snapshot> channels = queryChannels(
        "WHERE c.ied_device_id=:iedDeviceId",
        Collections.<String, Object>singletonMap("iedDeviceId", iedDeviceId));
    Map<Long, Map<String, Object>> associations = new LinkedHashMap<>();
    for (Snapshot channel : channels) {
      Map<String, Object> association = associations.get(channel.getAssociationId());
      if (association == null) {
        association = new LinkedHashMap<>();
        association.put("associationId", channel.getAssociationId());
        association.put("category", channel.getCategory());
        association.put("controlIdentityKey", channel.getControlIdentityKey());
        association.put("sourceIedName", channel.getSourceIedName());
        association.put("controlName", channel.getControlName());
        association.put("controlReference", channel.getControlReference());
        association.put("valid", channel.isControlBlockMatched());
        association.put("invalidReason", channel.isControlBlockMatched() ? null : "关联的 SV 控制块已失效");
        association.put("channels", new ArrayList<Map<String, Object>>());
        associations.put(channel.getAssociationId(), association);
      }
      Map<String, Object> item = new LinkedHashMap<>();
      item.put("channelId", channel.getChannelId());
      item.put("description", channel.getTelemetryDescription());
      item.put("reference", channel.getTelemetryReference());
      item.put("datasetName", channel.getDatasetName());
      item.put("valueType", channel.getValueType());
      item.put("valid", channel.isControlBlockMatched());
      item.put("invalidReason", channel.isControlBlockMatched() ? null : "关联的 SV 控制块已失效");
      @SuppressWarnings("unchecked")
      List<Map<String, Object>> items = (List<Map<String, Object>>) association.get("channels");
      items.add(item);
    }
    Map<String, Object> result = new LinkedHashMap<>();
    result.put("device", device);
    result.put("associations", new ArrayList<>(associations.values()));
    return result;
  }

  public Map<Long, Snapshot> loadCurrentChannels(Collection<Long> channelIds) {
    if (channelIds == null || channelIds.isEmpty()) return Collections.emptyMap();
    Map<String, Object> params = new HashMap<>();
    params.put("channelIds", channelIds);
    List<Snapshot> rows = queryChannels("WHERE c.id IN (:channelIds)", params);
    Map<Long, Snapshot> result = new LinkedHashMap<>();
    for (Snapshot row : rows) result.put(row.getChannelId(), row);
    return result;
  }

  public Map<String, Object> requireDevice(Long cabinetId, Long iedDeviceId) {
    requireCabinet(cabinetId);
    if (iedDeviceId == null || iedDeviceId <= 0) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "请选择有效装置");
    }
    Map<String, Object> params = new HashMap<>();
    params.put("cabinetId", cabinetId);
    params.put("iedDeviceId", iedDeviceId);
    List<Map<String, Object>> rows = jdbc.query(
        "SELECT id, cabinet_id, ied_name, ied_desc FROM ied_device WHERE id=:iedDeviceId",
        params,
        (rs, row) -> {
          Map<String, Object> item = new LinkedHashMap<>();
          item.put("id", rs.getLong("id"));
          item.put("cabinetId", rs.getLong("cabinet_id"));
          item.put("iedName", rs.getString("ied_name"));
          String description = rs.getString("ied_desc");
          item.put("displayName", description == null || description.trim().isEmpty()
              ? rs.getString("ied_name") : description);
          return item;
        });
    if (rows.isEmpty()) throw new ResponseStatusException(HttpStatus.NOT_FOUND, "装置不存在");
    Map<String, Object> device = rows.get(0);
    if (!Objects.equals(cabinetId, device.get("cabinetId"))) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "装置不属于当前屏柜");
    }
    return device;
  }

  private void requireCabinet(Long cabinetId) {
    if (cabinetId == null || cabinetId <= 0) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "屏柜 ID 无效");
    }
    Integer count = jdbc.queryForObject(
        "SELECT COUNT(*) FROM cabinet WHERE id=:cabinetId",
        Collections.singletonMap("cabinetId", cabinetId), Integer.class);
    if (count == null || count == 0) {
      throw new ResponseStatusException(HttpStatus.NOT_FOUND, "屏柜不存在");
    }
  }

  private List<Snapshot> queryChannels(String where, Map<String, ?> params) {
    String sql =
        "SELECT a.id association_id, a.ied_device_id, a.category, a.control_identity_key, "
            + "a.source_ied_name, a.control_name, a.control_reference, a.sort_order association_order, "
            + "c.id channel_id, c.telemetry_reference, c.telemetry_key, c.telemetry_description, "
            + "c.dataset_name, c.value_type, c.sort_order channel_order, cb.id current_control_block_id "
            + "FROM sampling_signal_channel c JOIN sampling_signal_association a ON a.id=c.association_id "
            + "JOIN ied_device d ON d.id=a.ied_device_id "
            + "LEFT JOIN scd_virtual_control_block cb ON BINARY cb.identity_key=BINARY a.control_identity_key "
            + "AND BINARY cb.receiver_ied_name=BINARY d.ied_name AND cb.service_type='SV' "
            + where + " ORDER BY CASE a.category WHEN 'VOLTAGE' THEN 0 ELSE 1 END, "
            + "a.sort_order, a.id, c.sort_order, c.id";
    return jdbc.query(sql, params, (rs, row) -> new Snapshot(
        rs.getLong("association_id"), rs.getLong("channel_id"), rs.getLong("ied_device_id"),
        rs.getString("category"), rs.getString("control_identity_key"),
        rs.getString("source_ied_name"), rs.getString("control_name"),
        rs.getString("control_reference"), rs.getString("telemetry_reference"),
        rs.getString("telemetry_key"), rs.getString("telemetry_description"),
        rs.getString("dataset_name"), rs.getString("value_type"),
        rs.getInt("association_order"), rs.getInt("channel_order"),
        rs.getObject("current_control_block_id") != null));
  }

  @Getter
  public static class Snapshot {
    private final long associationId;
    private final long channelId;
    private final long iedDeviceId;
    private final String category;
    private final String controlIdentityKey;
    private final String sourceIedName;
    private final String controlName;
    private final String controlReference;
    private final String telemetryReference;
    private final String telemetryKey;
    private final String telemetryDescription;
    private final String datasetName;
    private final String valueType;
    private final int associationOrder;
    private final int channelOrder;
    private final boolean controlBlockMatched;

    public Snapshot(
        long associationId, long channelId, long iedDeviceId, String category,
        String controlIdentityKey, String sourceIedName, String controlName,
        String controlReference, String telemetryReference, String telemetryKey,
        String telemetryDescription, String datasetName, String valueType,
        int associationOrder, int channelOrder, boolean controlBlockMatched) {
      this.associationId = associationId;
      this.channelId = channelId;
      this.iedDeviceId = iedDeviceId;
      this.category = category;
      this.controlIdentityKey = controlIdentityKey;
      this.sourceIedName = sourceIedName;
      this.controlName = controlName;
      this.controlReference = controlReference;
      this.telemetryReference = telemetryReference;
      this.telemetryKey = telemetryKey;
      this.telemetryDescription = telemetryDescription;
      this.datasetName = datasetName;
      this.valueType = valueType;
      this.associationOrder = associationOrder;
      this.channelOrder = channelOrder;
      this.controlBlockMatched = controlBlockMatched;
    }
  }
}
