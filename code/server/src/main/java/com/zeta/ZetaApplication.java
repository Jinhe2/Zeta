package com.zeta;

import com.zeta.config.BusinessDataSourceProperties;
import com.zeta.config.JwtProperties;
import com.zeta.config.ScreenDataSourceProperties;
import com.zeta.integration.queue.ScreenQueueProperties;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.boot.autoconfigure.jdbc.DataSourceTransactionManagerAutoConfiguration;
import org.springframework.boot.autoconfigure.orm.jpa.HibernateJpaAutoConfiguration;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication(exclude = {
        DataSourceAutoConfiguration.class,
        DataSourceTransactionManagerAutoConfiguration.class,
        HibernateJpaAutoConfiguration.class
})
@EnableConfigurationProperties({
        BusinessDataSourceProperties.class,
        ScreenDataSourceProperties.class,
        ScreenQueueProperties.class,
        JwtProperties.class
})
@EnableScheduling
public class ZetaApplication {

    public static void main(String[] args) {
        SpringApplication.run(ZetaApplication.class, args);
    }
}
