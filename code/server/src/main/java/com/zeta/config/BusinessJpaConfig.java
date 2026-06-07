package com.zeta.config;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.orm.jpa.EntityManagerFactoryBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.orm.jpa.JpaTransactionManager;
import org.springframework.orm.jpa.LocalContainerEntityManagerFactoryBean;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.EnableTransactionManagement;

import javax.persistence.EntityManagerFactory;
import javax.sql.DataSource;
import java.util.HashMap;
import java.util.Map;

@Configuration
@EnableTransactionManagement
@EnableJpaRepositories(
        basePackages = "com.zeta.business",
        entityManagerFactoryRef = "businessEntityManagerFactory",
        transactionManagerRef = "businessTransactionManager")
public class BusinessJpaConfig {

    public static final String PERSISTENCE_UNIT = "business";

    @Primary
    @Bean
    public DataSource businessDataSource(BusinessDataSourceProperties properties) {
        return DataSourceFactory.build(properties);
    }

    @Primary
    @Bean
    public LocalContainerEntityManagerFactoryBean businessEntityManagerFactory(
            EntityManagerFactoryBuilder builder,
            @Qualifier("businessDataSource") DataSource dataSource,
            BusinessDataSourceProperties properties) {
        Map<String, Object> jpaProps = baseJpaProperties(properties.isShowSql());
        jpaProps.put("hibernate.hbm2ddl.auto", properties.getDdlAuto());
        return builder
                .dataSource(dataSource)
                .packages("com.zeta.business")
                .persistenceUnit(PERSISTENCE_UNIT)
                .properties(jpaProps)
                .build();
    }

    @Primary
    @Bean
    public PlatformTransactionManager businessTransactionManager(
            @Qualifier("businessEntityManagerFactory") EntityManagerFactory entityManagerFactory) {
        return new JpaTransactionManager(entityManagerFactory);
    }

    static Map<String, Object> baseJpaProperties(boolean showSql) {
        Map<String, Object> props = new HashMap<>();
        props.put("hibernate.dialect", "org.hibernate.dialect.MariaDB103Dialect");
        props.put("hibernate.format_sql", true);
        props.put("hibernate.show_sql", showSql);
        props.put(
                "hibernate.physical_naming_strategy",
                "org.springframework.boot.orm.jpa.hibernate.SpringPhysicalNamingStrategy");
        props.put(
                "hibernate.implicit_naming_strategy",
                "org.springframework.boot.orm.jpa.hibernate.SpringImplicitNamingStrategy");
        return props;
    }
}
