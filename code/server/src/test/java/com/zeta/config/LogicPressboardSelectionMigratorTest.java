package com.zeta.config;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.mockito.ArgumentMatchers.*;
import com.zeta.business.entities.logicgroup.*;
import com.zeta.business.entities.pressboardselection.*;
import com.zeta.business.entities.settinglist.SettingListScopeType;
import com.zeta.business.entities.softpressboardlist.*;
import com.zeta.business.entities.hardpressboardlist.*;
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

class LogicPressboardSelectionMigratorTest {
  private static final String KEY = "unify_logic_pressboards_v22";
  private LogicPressboardSelectionMigrator migrator;
  private SoftPressboardListItemRepository soft;
  private HardPressboardListItemRepository hard;
  private LogicPressboardSelectionRepository selections;
  private ProtectionLogicRepository logics;
  private PlatformTransactionManager transactions;
  private AtomicBoolean completed;

  @BeforeEach
  void 初始化() {
    soft = mock(SoftPressboardListItemRepository.class); hard = mock(HardPressboardListItemRepository.class);
    selections = mock(LogicPressboardSelectionRepository.class); logics = mock(ProtectionLogicRepository.class);
    LogicGroupRepository groups = mock(LogicGroupRepository.class);
    SettingListTargetService targets = mock(SettingListTargetService.class);
    transactions = mock(PlatformTransactionManager.class);
    when(transactions.getTransaction(any())).thenReturn(new SimpleTransactionStatus());
    migrator = new LogicPressboardSelectionMigrator(mock(DataSource.class), transactions, logics, groups, targets, soft, hard, selections);
    JdbcTemplate jdbc = mock(JdbcTemplate.class);
    ReflectionTestUtils.setField(migrator, "jdbc", jdbc);
    completed = new AtomicBoolean(false);
    when(jdbc.queryForObject(anyString(), eq(Integer.class), eq(KEY))).thenAnswer(invocation -> completed.get() ? 1 : 0);
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
  void 保留旧勾选并分别初始化软硬压板且重启不覆盖() {
    when(soft.findByScopeTypeAndScopeIdOrderBySortOrderAscIdAsc(SettingListScopeType.IED_DEVICE, 12L))
        .thenReturn(Arrays.asList(soft("A", false), soft("B", true)));
    when(soft.findByScopeTypeAndScopeIdOrderBySortOrderAscIdAsc(SettingListScopeType.LOGIC_DIAGRAM, 10L))
        .thenReturn(Arrays.asList(soft("A", true), soft("B", false), soft("已删除", true)));
    when(hard.findByScopeTypeAndScopeIdOrderBySortOrderAscIdAsc(SettingListScopeType.IED_DEVICE, 12L))
        .thenReturn(Collections.singletonList(hard("1", true)));
    when(hard.findByScopeTypeAndScopeIdOrderBySortOrderAscIdAsc(SettingListScopeType.LOGIC_GROUP, 20L))
        .thenReturn(Collections.singletonList(hard("1", false)));
    migrator.run();
    ArgumentCaptor<LogicPressboardSelection> rows = ArgumentCaptor.forClass(LogicPressboardSelection.class);
    verify(selections, times(3)).save(rows.capture());
    List<String> summary = new ArrayList<>();
    rows.getAllValues().forEach(row -> summary.add(row.getPressboardKind() + ":" + row.getScopeId() + ":" + row.getPressboardRef()));
    assertEquals(Arrays.asList("SOFT:10:A", "HARD:10:1", "SOFT:20:B"), summary);
    assertTrue(completed.get());
    migrator.run();
    verify(logics, times(1)).findAllByOrderByIdAsc();
    verify(selections, times(3)).save(any());
    verify(soft, never()).save(any()); verify(hard, never()).save(any());
  }

  @Test
  void 装置空清单不从旧逻辑补建基准() {
    when(soft.findByScopeTypeAndScopeIdOrderBySortOrderAscIdAsc(SettingListScopeType.LOGIC_DIAGRAM, 10L))
        .thenReturn(Collections.singletonList(soft("旧压板", true)));
    migrator.run();
    verify(selections, never()).save(any());
    verify(soft, never()).save(any()); verify(hard, never()).save(any());
    assertTrue(completed.get());
  }

  @Test
  void 迁移失败回滚且不记录完成() {
    when(logics.findAllByOrderByIdAsc()).thenThrow(new IllegalStateException("屏柜不可用"));
    assertThrows(IllegalStateException.class, () -> migrator.run());
    assertFalse(completed.get()); verify(transactions).rollback(any());
  }

  private SoftPressboardListItem soft(String ref, boolean enabled) {
    SoftPressboardListItem item = new SoftPressboardListItem(); item.setPressboardRef(ref); item.setCompareEnabled(enabled); return item;
  }
  private HardPressboardListItem hard(String ref, boolean enabled) {
    HardPressboardListItem item = new HardPressboardListItem(); item.setPressboardRef(ref); item.setCompareEnabled(enabled); return item;
  }
}
