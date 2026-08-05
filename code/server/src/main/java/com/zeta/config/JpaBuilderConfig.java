package com.zeta.config;

import java.util.HashMap;
import org.springframework.boot.orm.jpa.EntityManagerFactoryBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.orm.jpa.vendor.HibernateJpaVendorAdapter;

@Configuration
public class JpaBuilderConfig {

    @Bean
    public EntityManagerFactoryBuilder entityManagerFactoryBuilder() {
        // 双数据源场景下各 EMF 在 BusinessJpaConfig / ScreenJpaConfig 中单独设置属性
        return new EntityManagerFactoryBuilder(
                new HibernateJpaVendorAdapter(),
                new HashMap<>(),
                null);
    }
}
