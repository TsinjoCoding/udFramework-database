package com.udframework.database.spring;

import com.udframework.database.ConnectionGetter;
import com.udframework.database.annotations.ConnectionProvider;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.SQLException;

@ConnectionProvider
public class DBConnection implements ConnectionGetter {

    @Override
    public Connection getConnection() throws SQLException {
        return ((DataSource)ApplicationContextProvider.getContext().getBean("dataSource")).getConnection();
    }

}
