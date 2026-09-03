package com.zeta.config;

import com.zeta.business.entities.logicgroup.LogicGroup;
import com.zeta.business.entities.logicgroup.LogicGroupRepository;
import com.zeta.business.entities.settinglist.*;
import com.zeta.business.service.SettingListTargetService;
import com.zeta.screen.logicdiagram.ProtectionLogic;
import com.zeta.screen.logicdiagram.ProtectionLogicRepository;
import java.util.*;
import java.util.stream.Collectors;
import javax.sql.DataSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

/** 一次性迁移旧清单的勾选；完成后新逻辑始终从空选择开始。 */
@Component
@Order(16)
public class LogicSettingSelectionMigrator implements CommandLineRunner {
  private static final Logger log = LoggerFactory.getLogger(LogicSettingSelectionMigrator.class);
  private static final String KEY = "unify_logic_settings_v21";
  private final JdbcTemplate jdbc;
  private final TransactionTemplate transaction;
  private final ProtectionLogicRepository logicRepository;
  private final LogicGroupRepository groupRepository;
  private final SettingListTargetService targetService;
  private final SettingListItemRepository settingRepository;
  private final LogicSettingSelectionRepository selectionRepository;

  public LogicSettingSelectionMigrator(
      @Qualifier("businessDataSource") DataSource dataSource,
      @Qualifier("businessTransactionManager") PlatformTransactionManager transactionManager,
      ProtectionLogicRepository logicRepository, LogicGroupRepository groupRepository,
      SettingListTargetService targetService, SettingListItemRepository settingRepository,
      LogicSettingSelectionRepository selectionRepository) {
    this.jdbc = new JdbcTemplate(dataSource);
    this.transaction = new TransactionTemplate(transactionManager);
    this.logicRepository = logicRepository;
    this.groupRepository = groupRepository;
    this.targetService = targetService;
    this.settingRepository = settingRepository;
    this.selectionRepository = selectionRepository;
  }

  @Override
  public void run(String... args) {
    transaction.execute(status -> {
      jdbc.update("INSERT INTO business_migration_state (migration_key, completed) VALUES (?, 0) "
          + "ON DUPLICATE KEY UPDATE migration_key = migration_key", KEY);
      Integer completed = jdbc.queryForObject(
          "SELECT completed FROM business_migration_state WHERE migration_key = ? FOR UPDATE", Integer.class, KEY);
      if (Integer.valueOf(1).equals(completed)) return null;
      for (ProtectionLogic logic : logicRepository.findAllByOrderByIdAsc()) {
        migrateScope(SettingListScopeType.LOGIC_DIAGRAM, logic.getId());
      }
      for (LogicGroup group : groupRepository.findAll()) {
        migrateScope(SettingListScopeType.LOGIC_GROUP, group.getId());
      }
      selectionRepository.flush();
      jdbc.update("UPDATE business_migration_state SET completed = 1 WHERE migration_key = ?", KEY);
      log.info("逻辑定值校验选择迁移完成，旧独立定值数据仅保留追溯");
      return null;
    });
  }

  private void migrateScope(SettingListScopeType type, Long id) {
    Long deviceId = targetService.require(type, id).getIedDeviceId();
    List<SettingListItem> deviceItems = settingRepository.findByScopeTypeAndScopeIdOrderBySortOrderAscIdAsc(
        SettingListScopeType.IED_DEVICE, deviceId);
    List<SettingListItem> oldItems = settingRepository.findByScopeTypeAndScopeIdOrderBySortOrderAscIdAsc(type, id);
    Set<String> available = deviceItems.stream().map(SettingListItem::getSettingRef).collect(Collectors.toSet());
    List<SettingListItem> source = oldItems.isEmpty() ? deviceItems : oldItems;
    Set<String> existing = selectionRepository.findByScopeTypeAndScopeId(type, id).stream()
        .map(LogicSettingSelection::getSettingRef).collect(Collectors.toSet());
    for (SettingListItem old : source) {
      if (!available.contains(old.getSettingRef())) {
        log.warn("逻辑 {} / {} 的旧定值 {} 不在装置清单中，未迁移该项目", type, id, old.getSettingRef());
        continue;
      }
      if (Boolean.FALSE.equals(old.getCompareEnabled()) || existing.contains(old.getSettingRef())) continue;
      LogicSettingSelection selection = new LogicSettingSelection();
      selection.setScopeType(type);
      selection.setScopeId(id);
      selection.setSettingRef(old.getSettingRef());
      selectionRepository.save(selection);
    }
    if (deviceItems.isEmpty()) log.warn("逻辑 {} / {} 所属装置 {} 暂无定值清单，校验选择为空", type, id, deviceId);
  }
}
