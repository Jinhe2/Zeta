package com.zeta.config;

import com.zaxxer.hikari.HikariDataSource;

import javax.sql.DataSource;

final class DataSourceFactory {

    private DataSourceFactory() {
    }

    static DataSource build(BusinessDataSourceProperties properties) {
        return build(
                properties.getUrl(),
                properties.getUsername(),
                properties.getPassword(),
                properties.getDriverClassName(),
                "business-pool");
    }

    static DataSource build(ScreenDataSourceProperties properties) {
        return build(
                properties.getUrl(),
                properties.getUsername(),
                properties.getPassword(),
                properties.getDriverClassName(),
                "screen-pool");
    }

    private static DataSource build(
            String url, String username, String password, String driverClassName, String poolName) {
        HikariDataSource dataSource = new HikariDataSource();
        dataSource.setJdbcUrl(url);
        dataSource.setUsername(username);
        dataSource.setPassword(password);
        dataSource.setDriverClassName(driverClassName);
        dataSource.setPoolName(poolName);
        dataSource.setMaximumPoolSize(10);
        dataSource.setMinimumIdle(1);
        return dataSource;
    }
}
