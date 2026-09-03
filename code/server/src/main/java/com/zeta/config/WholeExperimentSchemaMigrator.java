package com.zeta.config;

import javax.sql.DataSource;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.core.io.ClassPathResource;
import org.springframework.jdbc.datasource.init.ResourceDatabasePopulator;
import org.springframework.stereotype.Component;

/** 生产环境关闭自动建表时也创建整组实验业务表；失败阻止启动。 */
@Component
@Order(25)
public class WholeExperimentSchemaMigrator implements CommandLineRunner {
  private final DataSource dataSource;

  public WholeExperimentSchemaMigrator(@Qualifier("businessDataSource") DataSource dataSource) {
    this.dataSource = dataSource;
  }

  @Override
  public void run(String... args) {
    new ResourceDatabasePopulator(new ClassPathResource(
        "db/migration/V23__whole_experiments.sql")).execute(dataSource);
  }
}
