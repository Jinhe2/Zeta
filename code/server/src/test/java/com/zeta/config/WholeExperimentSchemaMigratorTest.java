package com.zeta.config;

import java.sql.*;
import java.util.List;
import javax.sql.DataSource;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class WholeExperimentSchemaMigratorTest {
  @Test void 启动迁移重复执行仅创建三张独立业务表() throws Exception {
    DataSource dataSource = mock(DataSource.class);
    Connection connection = mock(Connection.class);
    Statement statement = mock(Statement.class);
    when(dataSource.getConnection()).thenReturn(connection);
    when(connection.createStatement()).thenReturn(statement);
    when(connection.getAutoCommit()).thenReturn(true);
    WholeExperimentSchemaMigrator migrator = new WholeExperimentSchemaMigrator(dataSource);
    migrator.run();
    migrator.run();
    ArgumentCaptor<String> sql = ArgumentCaptor.forClass(String.class);
    verify(statement, times(6)).execute(sql.capture());
    List<String> values = sql.getAllValues();
    assertEquals(values.subList(0, 3), values.subList(3, 6));
    assertTrue(values.stream().allMatch(value -> value.startsWith("CREATE TABLE IF NOT EXISTS whole_experiment")));
    assertTrue(values.get(0).contains("uk_whole_experiment_owner"));
    assertTrue(values.get(2).contains("task_uuid VARCHAR(36) NOT NULL PRIMARY KEY"));
  }

  @Test void 建表失败阻止启动() throws Exception {
    DataSource dataSource = mock(DataSource.class);
    when(dataSource.getConnection()).thenThrow(new SQLException("测试数据库连接失败"));
    assertThrows(RuntimeException.class, () -> new WholeExperimentSchemaMigrator(dataSource).run());
  }
}
