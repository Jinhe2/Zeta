package com.zeta.config;

import com.zeta.business.entities.logicgroup.LogicGroup;
import com.zeta.business.entities.logicgroup.LogicGroupRepository;
import com.zeta.business.entities.settinglist.SettingListScopeType;
import com.zeta.business.entities.softpressboardlist.*;
import com.zeta.business.entities.hardpressboardlist.*;
import com.zeta.business.entities.pressboardselection.*;
import java.util.function.Function;
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
@Order(17)
public class LogicPressboardSelectionMigrator implements CommandLineRunner {
  private static final Logger log = LoggerFactory.getLogger(LogicPressboardSelectionMigrator.class);
  private static final String KEY = "unify_logic_pressboards_v22";
  private final JdbcTemplate jdbc;
  private final TransactionTemplate transaction;
  private final ProtectionLogicRepository logicRepository;
  private final LogicGroupRepository groupRepository;
  private final SettingListTargetService targetService;
  private final SoftPressboardListItemRepository softRepository;
  private final HardPressboardListItemRepository hardRepository;
  private final LogicPressboardSelectionRepository selectionRepository;

  public LogicPressboardSelectionMigrator(
      @Qualifier("businessDataSource") DataSource dataSource,
      @Qualifier("businessTransactionManager") PlatformTransactionManager transactionManager,
      ProtectionLogicRepository logicRepository, LogicGroupRepository groupRepository,
      SettingListTargetService targetService, SoftPressboardListItemRepository softRepository,
      HardPressboardListItemRepository hardRepository,
      LogicPressboardSelectionRepository selectionRepository) {
    this.jdbc = new JdbcTemplate(dataSource);
    this.transaction = new TransactionTemplate(transactionManager);
    this.logicRepository = logicRepository;
    this.groupRepository = groupRepository;
    this.targetService = targetService;
    this.softRepository = softRepository;
    this.hardRepository = hardRepository;
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
      log.info("逻辑压板校验选择迁移完成，旧独立压板数据仅保留追溯");
      return null;
    });
  }

  private void migrateScope(SettingListScopeType type, Long id) {
    Long deviceId = targetService.require(type, id).getIedDeviceId();
    migrateKind(PressboardKind.SOFT, type, id,
        softRepository.findByScopeTypeAndScopeIdOrderBySortOrderAscIdAsc(SettingListScopeType.IED_DEVICE, deviceId),
        softRepository.findByScopeTypeAndScopeIdOrderBySortOrderAscIdAsc(type, id),
        SoftPressboardListItem::getPressboardRef, SoftPressboardListItem::getCompareEnabled);
    migrateKind(PressboardKind.HARD, type, id,
        hardRepository.findByScopeTypeAndScopeIdOrderBySortOrderAscIdAsc(SettingListScopeType.IED_DEVICE, deviceId),
        hardRepository.findByScopeTypeAndScopeIdOrderBySortOrderAscIdAsc(type, id),
        HardPressboardListItem::getPressboardRef, HardPressboardListItem::getCompareEnabled);
  }

  private <T> void migrateKind(PressboardKind kind, SettingListScopeType type, Long id,
      List<T> deviceItems, List<T> oldItems, Function<T, String> reference, Function<T, Boolean> enabled) {
    Set<String> available = deviceItems.stream().map(reference).collect(Collectors.toSet());
    List<T> source = oldItems.isEmpty() ? deviceItems : oldItems;
    Set<String> existing = selectionRepository.findByPressboardKindAndScopeTypeAndScopeId(kind, type, id).stream()
        .map(LogicPressboardSelection::getPressboardRef).collect(Collectors.toSet());
    for (T old : source) {
      String ref = reference.apply(old);
      if (!available.contains(ref)) {
        log.warn("逻辑 {} / {} 的旧{}压板 {} 不在装置清单中，未迁移该项目", type, id,
            kind == PressboardKind.SOFT ? "软" : "硬", ref);
        continue;
      }
      if (Boolean.FALSE.equals(enabled.apply(old)) || !existing.add(ref)) continue;
      LogicPressboardSelection selection = new LogicPressboardSelection();
      selection.setPressboardKind(kind); selection.setScopeType(type); selection.setScopeId(id); selection.setPressboardRef(ref);
      selectionRepository.save(selection);
    }
    if (deviceItems.isEmpty()) log.warn("逻辑 {} / {} 所属装置暂无{}压板清单，校验选择为空", type, id,
        kind == PressboardKind.SOFT ? "软" : "硬");
  }
}
