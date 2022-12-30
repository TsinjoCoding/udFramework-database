package com.udframework.database;

import java.sql.Connection;
import java.sql.ResultSet;

import java.sql.SQLException;
import java.sql.Statement;

public class DBUtils {

    public static void execute(String sql, Connection connection) throws SQLException {
        try (Statement statement = connection.createStatement()) {
            statement.execute(sql);
        }
    }

    public static <T> T oneValue(String sql, Class<T> valType, Connection connection) throws SQLException {
        try (Statement statement = connection.createStatement()) {
            try (ResultSet resultSet = statement.executeQuery(sql)) {
                resultSet.next();
                return (T) resultSet.getObject(1);
            }
        }
    }

}
