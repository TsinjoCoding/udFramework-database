package com.udframework.bdd;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Collections;

import com.udframework.bdd.exceptions.DatabaseException;

public class Temp<E extends DBObject> {

    final E obj;
    final String defaultSelect;
    final ArrayList<String> conditions = new ArrayList<>();
    final Connection connection;
    final ArrayList<String> orders = new ArrayList<>();

    protected boolean closeConnection = false;

    public Temp(String defaultSelect, Connection connection, E obj, boolean closeConnection) {
        this.obj = obj;
        this.defaultSelect = defaultSelect;
        this.connection = connection;
        this.closeConnection = closeConnection;
    }

    public Temp(String defaultSelect, Connection connection, E obj) {
        this.defaultSelect = defaultSelect;
        this.connection = connection;
        this.obj = obj;
    }

    public Temp<E> where(String... cond_s) {
        Collections.addAll(this.conditions, cond_s);
        return this;
    }

    public Temp<E> where (String col, Object value) {
        this.conditions.add(col + " = " + value);
        return this;
    }

    protected String buildQuery() {
        return defaultSelect
                + (conditions.size() != 0 ? " where " + buildConditions() : "")
                + (orders.size() != 0 ? " order by " + buildOrder() : "");
    }

    private String buildConditions() {
        StringBuilder cond = new StringBuilder();

        for (int i = 0; i < conditions.size(); i++) {
            if (i != 0) {
                cond.append(" and ");
            }
            cond.append(conditions.get(i));
        }

        return cond.toString();
    }

    private String buildOrder() {
        String order = orders.toString();
        return order.substring(1, order.length() - 1);
    }

    public ArrayList<E> run() throws ReflectiveOperationException, SQLException, DatabaseException {
        try {
            return obj.runQuery(buildQuery(), this.connection);
        }
        finally {
            if (closeConnection) {
                this.connection.close();
            }
        }
    }

    public Temp<E> orderBy(String... columns_order) {
        Collections.addAll(this.orders, columns_order);
        return this;
    }

    public <T> T max(String column, T type) throws SQLException {
        return oneValue("max", column, type);
    }

    public <T> T min(String column, T type) throws SQLException {
        return oneValue("min", column, type);
    }

    public long count() throws SQLException, DatabaseException {
        return oneValue("count", obj.pk.getColumnName(), Long.parseLong("0"));
    }

    private <T> T oneValue(String fn, String column, T type) throws SQLException {
        String query = buildQuery();
        query = String.format("select %s(%s) from (%s) as t", fn, column, query);

        try (Statement statement = this.connection.createStatement();
                ResultSet resultSet = statement.executeQuery(query)) {
            if (resultSet.next()) {
                return (T) resultSet.getObject(1);
            }
            return null;
        }
    }

    public void closeAfterRun() {
        this.closeConnection = true;
    }
}
