package com.zeta.config;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.mockito.ArgumentMatchers.*;

import com.zeta.business.entities.logicgroup.*;
import com.zeta.business.entities.settinglist.*;
import com.zeta.business.service.SettingListTargetService;
import com.zeta.business.service.SettingListTargetService.Target;
import com.zeta.screen.logicdiagram.*;
import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;
import javax.sql.DataSource;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.SimpleTransactionStatus;

class LogicSettingSelectionMigratorTest {
  private static final String KEY = "unify_logic_settings_v21";
  private LogicSettingSelectionMigrator migrator;
  private SettingListItemRepository settings;
  private LogicSettingSelectionRepository selections;
  private ProtectionLogicRepository logics;
  private JdbcTemplate jdbc;
  private PlatformTransactionManager transactions;
  private AtomicBoolean completed;

  @BeforeEach
  void 初始化() {
    settings = mock(SettingListItemRepository.class);
    selections = mock(LogicSettingSelectionRepository.class);
    logics = mock(ProtectionLogicRepository.class);
    LogicGroupRepository groups = mock(LogicGroupRepository.class);
    SettingListTargetService targets = mock(SettingListTargetService.class);
    transactions = mock(PlatformTransactionManager.class);
    when(transactions.getTransaction(any())).thenReturn(new SimpleTransactionStatus());
    migrator = new LogicSettingSelectionMigrator(mock(DataSource.class), transactions, logics, groups, targets, settings, selections);
    jdbc = mock(JdbcTemplate.class);
    ReflectionTestUtils.setField(migrator, "jdbc", jdbc);
    completed = new AtomicBoolean(false);
    when(jdbc.queryForObject(anyString(), eq(Integer.class), eq(KEY)))
        .thenAnswer(invocation -> completed.get() ? 1 : 0);
    when(jdbc.update("UPDATE business_migration_state SET completed = 1 WHERE migration_key = ?", KEY))
        .thenAnswer(invocation -> { completed.set(true); return 1; });
    ProtectionLogic logic = new ProtectionLogic(); logic.setId(10L);
    LogicGroup group = new LogicGroup(); group.setId(20L);
    when(logics.findAllByOrderByIdAsc()).thenReturn(Collections.singletonList(logic));
    when(groups.findAll()).thenReturn(Collections.singletonList(group));
    when(targets.require(SettingListScopeType.LOGIC_DIAGRAM, 10L))
        .thenReturn(new Target(SettingListScopeType.LOGIC_DIAGRAM, 10L, "基础逻辑", 12L, "装置", 1L));
    when(targets.require(SettingListScopeType.LOGIC_GROUP, 20L))
        .thenReturn(new Target(SettingListScopeType.LOGIC_GROUP, 20L, "组合逻辑", 12L, "装置", 1L));
  }

  @Test
  void 独立清单只保留有效勾选而原回退逻辑沿用装置勾选() {
    when(settings.findByScopeTypeAndScopeIdOrderBySortOrderAscIdAsc(SettingListScopeType.IED_DEVICE, 12L))
        .thenReturn(Arrays.asList(item("A", false), item("B", true)));
    when(settings.findByScopeTypeAndScopeIdOrderBySortOrderAscIdAsc(SettingListScopeType.LOGIC_DIAGRAM, 10L))
        .thenReturn(Arrays.asList(item("A", true), item("B", false), item("已移除", true)));
    migrator.run();
    ArgumentCaptor<LogicSettingSelection> rows = ArgumentCaptor.forClass(LogicSettingSelection.class);
    verify(selections, times(2)).save(rows.capture());
    assertEquals(SettingListScopeType.LOGIC_DIAGRAM, rows.getAllValues().get(0).getScopeType());
    assertEquals("A", rows.getAllValues().get(0).getSettingRef());
    assertEquals(SettingListScopeType.LOGIC_GROUP, rows.getAllValues().get(1).getScopeType());
    assertEquals("B", rows.getAllValues().get(1).getSettingRef());
    assertTrue(completed.get());
    verify(settings, never()).save(any());
    verify(settings, never()).delete(any());
    // 再次启动不得覆盖迁移后管理员的勾选。
    migrator.run();
    verify(logics, times(1)).findAllByOrderByIdAsc();
    verify(selections, times(2)).save(any());
  }

  @Test
  void 旧清单全部不勾选时不继承装置勾选() {
    when(settings.findByScopeTypeAndScopeIdOrderBySortOrderAscIdAsc(SettingListScopeType.IED_DEVICE, 12L))
        .thenReturn(Collections.singletonList(item("A", true)));
    when(settings.findByScopeTypeAndScopeIdOrderBySortOrderAscIdAsc(SettingListScopeType.LOGIC_DIAGRAM, 10L))
        .thenReturn(Collections.singletonList(item("A", false)));
    when(settings.findByScopeTypeAndScopeIdOrderBySortOrderAscIdAsc(SettingListScopeType.LOGIC_GROUP, 20L))
        .thenReturn(Collections.singletonList(item("A", false)));
    migrator.run();
    verify(selections, never()).save(any());
    assertTrue(completed.get());
  }

  @Test
  void 装置清单为空时不自动补建定值() {
    when(settings.findByScopeTypeAndScopeIdOrderBySortOrderAscIdAsc(SettingListScopeType.LOGIC_DIAGRAM, 10L))
        .thenReturn(Collections.singletonList(item("旧定值", true)));
    migrator.run();
    verify(selections, never()).save(any());
    verify(settings, never()).save(any());
    assertTrue(completed.get());
  }

  @Test
  void 迁移失败不写完成标记并回滚业务事务() {
    when(logics.findAllByOrderByIdAsc()).thenThrow(new IllegalStateException("屏柜数据暂不可用"));
    assertThrows(IllegalStateException.class, () -> migrator.run());
    assertFalse(completed.get());
    verify(transactions).rollback(any());
    verify(transactions, never()).commit(any());
  }

  private SettingListItem item(String ref, boolean enabled) {
    SettingListItem item = new SettingListItem();
    item.setSettingRef(ref); item.setCompareEnabled(enabled);
    return item;
  }
}
