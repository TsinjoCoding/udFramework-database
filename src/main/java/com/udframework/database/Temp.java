package com.udframework.database;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import com.udframework.database.exceptions.DatabaseException;

public class Temp <E extends DBObject> {
    final E obj;
    final String defaultSelect;
    ArrayList<Object> values;
    ArrayList<String> conditions;

    private void append (Object val) {
        if (values == null) values = new ArrayList<>();
        values.add(val);
    }

    private void appendClause (String clause){
        if (conditions == null) conditions = new ArrayList<>();
        this.conditions.add(clause);
    }

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


    final Temp<E> compare (String col, Object value, Operator operator) throws DatabaseException {
        checkColumn(col);
        appendClause(col + operator + " ? " );
        append(value);
        return this;
    }

    public Temp<E> isEqual(String col, Object value) throws DatabaseException {
        return compare(col, value, Operator.EQ);
    }

    public Temp<E> isGreaterThan (String col, Object value) throws Exception {
        compare(col, value, Operator.SUP);
        return this;
    }

    public Temp<E> isLessThan (String col, Object value) throws Exception {
        compare(col, value, Operator.INF);
        return this;
    }

    public Temp<E> isGreaterOrEqualThan (String col, Object value) throws Exception {
        compare(col, value, Operator.SUP_EQ);
        return this;
    }

    public Temp<E> isLessOrEqualThan (String col, Object value) throws Exception {
        compare(col, value, Operator.INF_EQ);
        return this;
    }

    public Temp<E> isNotEqual (String col, Object value) throws Exception {
        compare(col, value, Operator.NEQ);
        return this;
    }

    public Temp<E> isLike (String col, Object value) throws Exception {
        compare(col, value, Operator.LIKE);
        return this;
    }

    public Temp<E> isNotLike (String col, Object value) throws Exception {
        compare(col, value, Operator.NOT_LIKE);
        return this;
    }

    public Temp<E> isNull (String col) throws Exception {
        checkColumn(col);
        appendClause(col + Operator.IS_NULL);
        return this;
    }

    public Temp<E> isNotNull (String col) throws Exception {
        checkColumn(col);
        appendClause(col + Operator.IS_NOT_NULL);
        return this;
    }

    public Temp<E> isBetween (String col, Object value1, Object value2) throws Exception {
        checkColumn(col);
        appendClause(col + "between  ? and ? ");
        append(value1);
        append(value2);
        return this;
    }

    public Temp<E> isNotBetween (String col, Object value1, Object value2) throws Exception {
        checkColumn(col);
        appendClause(col + "not between  ? and ? ");
        append(value1);
        append(value2);
        return this;
    }

    public Temp<E> isTrue (String col) throws Exception {
        checkColumn(col);
        appendClause(col + "is true");
        return this;
    }

    public Temp<E> isFalse (String col) throws Exception {
        checkColumn(col);
        appendClause(col + "is false");
        return this;
    }

    public Temp<E> isILike (String col, Object value) throws Exception {
        compare(col, value, Operator.I_LIKE);
        return this;
    }

    public Temp<E> in(String col, List<Object> values) throws Exception {
        return inOrNot(col, values, true);
    }

    public Temp<E> notIn(String col, List<Object> values) throws Exception {
        return inOrNot(col, values, false);
    }

    private Temp<E> inOrNot(String col, List<Object> values, boolean in) throws Exception {
        checkColumn(col);
        appendClause(col + (in ? "": " not ") + " in " +" ( " + String.join(", ", Collections.nCopies(values.size(), "?")) + " )");
        for (Object value : values) {
            append(value);
        }
        return this;
    }

    public void checkColumn (String col) throws DatabaseException {
        Object field = obj.classData.getFieldMetadata(col);
        if (field == null) throw new DatabaseException("field "+col+ " do noo exist");
    }

    protected String buildQuery() {
        return defaultSelect
                + (conditions != null && conditions.size() != 0 ? " where " + buildConditions() : "")
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
            return obj.runQuery(buildQuery(), getValues(), this.connection);
        }
        finally {
            if (closeConnection) {
                this.connection.close();
            }
        }
    }

    protected List<Object> getValues() {
        return values == null ? new ArrayList<>() : values;
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
