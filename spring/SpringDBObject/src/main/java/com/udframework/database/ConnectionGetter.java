package com.udframework.database;

import java.sql.Connection;
import java.sql.SQLException;

public interface ConnectionGetter {
    Connection getConnection() throws SQLException;
}
