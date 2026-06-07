package com.zeta.config;

import org.springframework.jdbc.datasource.DelegatingDataSource;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.SQLException;

/**
 * 标记屏柜库连接为只读（hint）。生产环境应对 ct-screen 账号仅授予 SELECT。
 */
public class ReadOnlyDataSource extends DelegatingDataSource {

    public ReadOnlyDataSource(DataSource target) {
        super(target);
    }

    @Override
    public Connection getConnection() throws SQLException {
        Connection connection = super.getConnection();
        connection.setReadOnly(true);
        return connection;
    }

    @Override
    public Connection getConnection(String username, String password) throws SQLException {
        Connection connection = super.getConnection(username, password);
        connection.setReadOnly(true);
        return connection;
    }
}
