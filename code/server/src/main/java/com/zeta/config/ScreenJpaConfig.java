package com.zeta.config;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.orm.jpa.EntityManagerFactoryBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.orm.jpa.JpaTransactionManager;
import org.springframework.orm.jpa.LocalContainerEntityManagerFactoryBean;
import org.springframework.transaction.PlatformTransactionManager;

import javax.persistence.EntityManagerFactory;
import javax.sql.DataSource;
import java.util.HashMap;
import java.util.Map;

@Configuration
@EnableJpaRepositories(
        basePackages = {
                "com.zeta.screen.cabinet",
                "com.zeta.screen.ieddevice",
                "com.zeta.screen.logicdiagram",
                "com.zeta.screen.terminal",
                "com.zeta.screen.hardpressboard",
                "com.zeta.screen.baseline",
                "com.zeta.screen.wiring",
                "com.zeta.screen.monitor"
        },
        entityManagerFactoryRef = "screenEntityManagerFactory",
        transactionManagerRef = "screenTransactionManager")
public class ScreenJpaConfig {

    public static final String PERSISTENCE_UNIT = "screen";

    @Bean
    public DataSource screenDataSource(ScreenDataSourceProperties properties) {
        DataSource dataSource = DataSourceFactory.build(properties);
        if (properties.isReadOnly()) {
            return new ReadOnlyDataSource(dataSource);
        }
        return dataSource;
    }

    @Bean
    public LocalContainerEntityManagerFactoryBean screenEntityManagerFactory(
            EntityManagerFactoryBuilder builder,
            @Qualifier("screenDataSource") DataSource dataSource,
            ScreenDataSourceProperties properties) {
        Map<String, Object> jpaProps = BusinessJpaConfig.baseJpaProperties(properties.isShowSql());
        jpaProps.put("hibernate.hbm2ddl.auto", properties.getDdlAuto());
        return builder
                .dataSource(dataSource)
                .packages(
                        "com.zeta.screen.cabinet",
                        "com.zeta.screen.ieddevice",
                        "com.zeta.screen.logicdiagram",
                        "com.zeta.screen.terminal",
                        "com.zeta.screen.hardpressboard",
                        "com.zeta.screen.baseline",
                        "com.zeta.screen.wiring",
                        "com.zeta.screen.monitor")
                .persistenceUnit(PERSISTENCE_UNIT)
                .properties(jpaProps)
                .build();
    }

    @Bean
    public PlatformTransactionManager screenTransactionManager(
            @Qualifier("screenEntityManagerFactory") EntityManagerFactory entityManagerFactory) {
        return new JpaTransactionManager(entityManagerFactory);
    }
}
