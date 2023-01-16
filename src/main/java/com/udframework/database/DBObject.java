package com.udframework.database;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.udframework.database.annotations.ManyToMany;
import com.udframework.database.annotations.OneToMany;
import com.udframework.database.annotations.OneToOne;
import com.udframework.database.connectionHandler.ConnectionHandler;
import com.udframework.database.exceptions.DatabaseException;
import com.udframework.database.generic.ClassMetadata;
import com.udframework.database.generic.FieldMetadata;
import com.udframework.database.generic.RelationField;
import com.udframework.database.generic.relationTypes.RelationType;
import com.udframework.database.generic.utils.StringUtils;

import java.lang.annotation.Annotation;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.HashMap;

public class DBObject<E extends DBObject<E, P>, P> implements DBValidation {

    static final String defaultInsert = "insert into %s (%s) values (%s)";
    static final String defaultUpdate = "Update %s set %s where %s";
    static final String defaultSelect = "select %s from %s";
    static final String defaultDelete = "delete from %s where %s";
    static final ObjectMapper mapper = new ObjectMapper();
    @JsonIgnore
    protected final ClassMetadata classData;
    final FieldMetadata pk;
    final String instanceSelect;

    public DBObject() throws DatabaseException, NoSuchMethodException {
        classData = ClassMetadata.getMetadataOf(getClass());
        pk = classData.getPrimaryKey();
        instanceSelect = String.format(defaultSelect, classData.getAllColumns(), classData.getTableName());
    }

    public void populateColumn(String col, Connection connection) throws Exception {
        FieldMetadata fk = classData.getFieldMetadata(col.toLowerCase());
        if (fk == null) {
            throw new DatabaseException(String.format("%s cannot be populated", col));
        }
        Object fkValue = fk.valueIn(this);
        ClassMetadata fkData = ClassMetadata.getMetadataOf(fk.getType());
        DBObject<?, ?> pop = ((DBObject) fkData.newInstance()).findById(connection, fkValue);
        fk.setValueIn(this, pop);
    }

    public void populateColumn(Connection connection, String... cols) throws Exception {
        for (String col : cols) {
            this.populateColumn(col, connection);
        }
    }

    public void populateField(String field, Connection connection)
            throws DatabaseException, ReflectiveOperationException, SQLException {
        RelationField relationField = classData.relations.get(field.toLowerCase());
        if (relationField == null) {
            throw new DatabaseException(String.format("%s cannot be populated", field));
        }
        Object value = valueFrom(relationField, connection);
        relationField.setValueIn(this, value);
    }

    private Object valueFrom(RelationField relationField, Connection connection)
            throws DatabaseException, ReflectiveOperationException, SQLException {
        RelationType relationType = relationField.getRelationType();

        if (relationType.isOne()) {
            OneToOne oneT1 = (OneToOne) relationType.getAnnotation();
            FieldMetadata from = classData.getFieldMetadata(oneT1.from());
            ClassMetadata cm = ClassMetadata.getMetadataOf(relationField.getType());

            // instance of the field value
            ArrayList<?> result = ((DBObject<?, ?>) cm.newInstance())
                    .fetchAll(connection)
                    .where(String.format("%s='%s'", StringUtils.wrap(oneT1.joinTo().toLowerCase()), from.valueIn(this)))
                    .run();
            return result.size() == 0 ? null : (DBObject<?, ?>) result.get(0);
        }

        return valuesFrom(relationField, connection);
    }

    private Object valuesFrom(RelationField relationField, Connection connection) throws DatabaseException, ReflectiveOperationException, SQLException {
        String subQuery;
        ClassMetadata cd;
        DBObject<?, ?> obj;
        RelationType type = relationField.getRelationType();
        Annotation annotation = type.getAnnotation();

        if (annotation instanceof OneToMany) {
            OneToMany oneTM = (OneToMany) annotation;
            cd = ClassMetadata.getMetadataOf(oneTM.elt());
            subQuery = String.format(defaultSelect, cd.getPrimaryKey().getColumnName(), cd.getTableName());
            subQuery = subQuery + String.format(
                " where %s = '%s'",
                StringUtils.wrap(oneTM.to()),
                classData.getFieldMetadata(oneTM.from()).valueIn(this)
            );
        } else {
            ManyToMany mTm = (ManyToMany) annotation;
            cd = ClassMetadata.getMetadataOf(mTm.elt());

            subQuery = "select %s from %s t1 join %s t2 on t1.%s = t2.%s where t2.%s = '%s'";
            subQuery = String.format(
                subQuery,
                StringUtils.wrap(mTm.extern().from()),
                StringUtils.wrap(cd.getTableName()),
                StringUtils.wrap(mTm.linkTable()),
                StringUtils.wrap(mTm.extern().from()),
                StringUtils.wrap(mTm.extern().to()),
                StringUtils.wrap(mTm.intern().to()),
                classData.getFieldMetadata(mTm.intern().from()).valueIn(this)
            );
        }

        obj = (DBObject<?, ?>) cd.newInstance();

        return obj.fetchAll(connection)
                .where(
            String.format(
                    " %s in ( %s )",
                    cd.getPrimaryKey().getColumnName(),
                    subQuery)
                ).run();
    }

    public P nextGeneratedValue(String columnName, Connection connection) throws SQLException, DatabaseException {
        return nextGeneratedValue(columnName, this.classData, connection);
    }

    public P nextGeneratedValue(String columnName, ClassMetadata classData, Connection connection) throws DatabaseException, SQLException {
        Long seq = callSequence(classData.getFieldMetadata(columnName), connection);
        FieldMetadata field = classData.getFieldMetadata(columnName);
        return (P) field.buildGeneratedValue(seq);
    }

    private Long callSequence(FieldMetadata field, Connection connection) throws SQLException, DatabaseException {

        if (!field.isGenerated()) {
            throw new DatabaseException(String.format("%s cant be generated", field.getColumnName()));
        }
        return field.sequence().nextVal(connection);
    }

    protected E findById(Connection connection, P id, ClassMetadata classData) throws Exception {
        if (pk == null) throw new DatabaseException("no primary key found");
        ArrayList<E> results = fetchAll(connection, classData).where(String.format("%s='%s'", classData.getPrimaryKey().getColumnName(), id)).run();
        return results.size() == 0 ? null : results.get(0);
    }

    public E findById(P id) throws Exception {
        try (Connection connection = ConnectionHandler.createSession()) {
            return findById(connection, id);
        }
    }

    public E findById(Connection connection, P id) throws Exception {
        return findById(connection, id, this.classData);
    }

    public Temp<E> fetchAll() throws Exception {
        Connection connection = ConnectionHandler.createSession();
        Temp<E> temp = fetchAll(connection);
        temp.closeAfterRun();
        return temp;
    }

    public <T extends E> Temp<T> fetchAll(Class<T> clazz) throws Exception {
        Connection connection = ConnectionHandler.createSession();
        Temp<T> temp = fetchAll(connection, clazz);
        temp.closeAfterRun();
        return temp;
    }

    public <T extends E> Temp<T> fetchAll(Connection connection, Class<T> clazz) throws Exception {
        return new Temp<T>(instanceSelect, connection, (T) this);
    }

    public Temp<E> fetchAll(Connection connection) {
        return fetchAll(connection, this.classData);
    }

    public Temp<E> fetchAll(Connection connection, ClassMetadata classData) {
        return new Temp<E>(instanceSelect, connection, (E) this);
    }

    /*
     * used for fetching data with the same structure but different table
     */
    public Temp<E> fetchAll(Connection connection, String table) {
        String sql = String.format(defaultSelect, classData.getAllColumns(), table);
        return new Temp<>(sql, connection, (E) this);
    }

    public <T extends E> Temp<T> fetchAll(Connection connection, String table, Class<T> clazz) {
        String sql = String.format(defaultSelect, classData.getAllColumns(), table);
        return new Temp<T>(sql, connection, (T) this);
    }

    public void delete() throws Exception {
        try (Connection connection = ConnectionHandler.createSession()) {
            delete(connection);
        }
    }

    public void delete(Connection connection) throws Exception {
        delete(connection, classData);
    }

    public void delete(Connection connection, ClassMetadata classData) throws Exception {
        if (classData.getPrimaryKey() == null) throw new DatabaseException("no primary key found");
        validateDelete(connection);
        DBUtils.execute(getDeleteQuery(classData), connection);
    }

    private String getDeleteQuery() throws ReflectiveOperationException, DatabaseException {
        return getDeleteQuery(this.classData);
    }

    protected String getDeleteQuery(ClassMetadata classData) throws ReflectiveOperationException, DatabaseException {
        if (classData.getDeleteTable().isEmpty()) {
            return String.format(defaultDelete, classData.getTableName(), getPrimaryKeyCondition(classData));
        }
        return String.format(defaultInsert, classData.getDeleteTable(), classData.getDeleteColumn(), getPkValue(classData));
    }

    public void create() throws Exception {
        try (Connection connection = ConnectionHandler.createSession()) {
            create(connection);
        }
    }

    public void create(Connection connection) throws Exception {
        create(connection, this.classData);
    }

    protected void create(Connection connection, ClassMetadata classData) throws Exception {
        validateCreate(connection);
        setBlankValues(connection, classData);
        DBUtils.execute(getInsertQuery(classData), connection);
    }

    private void setBlankValues(Connection connection) throws SQLException, DatabaseException, ReflectiveOperationException {
        setBlankValues(connection, this.classData);
    }

    protected void setBlankValues(Connection connection, ClassMetadata classData) throws ReflectiveOperationException, DatabaseException, SQLException {
        String[] columns = classData.getFields().keySet().toArray(new String[0]);

        for (String column : columns) {
            FieldMetadata fieldMetadata = classData.getFieldMetadata(column);
            if (fieldMetadata.valueIn(this) == null) {
                fieldMetadata.setDefaultIn(this, classData, connection);
            }
        }
    }

    public void update() throws Exception {
        try (Connection connection = ConnectionHandler.createSession()) {
            update(connection);
        }
    }

    public void update(Connection connection) throws Exception {
        update(connection, this.classData);
    }

    public void update(Connection connection, ClassMetadata classData) throws Exception {
        if (classData.getPrimaryKey() == null) throw new DatabaseException("no primary key found");
        validateUpdate(connection);
        DBUtils.execute(getUpdateQuery(classData), connection);
    }

    private String getInsertQuery() throws DatabaseException, ReflectiveOperationException {
        return getInsertQuery(this.classData);
    }

    protected String getInsertQuery(ClassMetadata classData) throws ReflectiveOperationException, DatabaseException {
        return String.format(defaultInsert, classData.getTableName(), classData.getAllColumns(), classData.getAllValues(this));
    }

    private String getUpdateQuery() throws DatabaseException, ReflectiveOperationException {
        return getUpdateQuery(this.classData);
    }

    protected String getUpdateQuery(ClassMetadata classData) throws DatabaseException, ReflectiveOperationException {
        ArrayList<String> updates = new ArrayList<>();

        HashMap<String, FieldMetadata> fields = classData.getFields();

        for (String columnName : fields.keySet()) {
            FieldMetadata fieldData = fields.get(columnName);

            if (!fieldData.isPrimaryKey()) {
                updates.add(columnName + " = " + ClassMetadata.valueToString(fieldData.valueIn(this)));
            }
        }

        return String.format(
                defaultUpdate,
                classData.getTableName(),
                updates.toString().substring(1, updates.toString().length() - 1),
                getPrimaryKeyCondition(classData)
        );
    }

    private Object getPkValue() throws DatabaseException, ReflectiveOperationException {
        return getPkValue(this.classData);
    }

    protected Object getPkValue(ClassMetadata classData) throws DatabaseException, ReflectiveOperationException {
        return classData.getPrimaryKey().valueIn(this);
    }

    String getPrimaryKeyCondition() throws DatabaseException, ReflectiveOperationException {
        return getPrimaryKeyCondition(this.classData);
    }

    String getPrimaryKeyCondition(ClassMetadata classData) throws DatabaseException, ReflectiveOperationException {
        FieldMetadata pk = classData.getPrimaryKey();
        return pk.getColumnName() + " = " + ClassMetadata.valueToString(pk.valueIn(this));
    }

    protected ArrayList<E> runQuery(String query, Connection connection) throws SQLException, ReflectiveOperationException, DatabaseException {
        return runQuery(query, connection, this.classData.getListFields().toArray(new FieldMetadata[0]));
    }

    protected ArrayList<E> runQuery(String query, Connection connection, FieldMetadata... fields) throws SQLException, ReflectiveOperationException, DatabaseException {
        ArrayList<E> results = new ArrayList<>();
        try (Statement statement = connection.createStatement()) {
            try (ResultSet resultSet = statement.executeQuery(query)) {
                while (resultSet.next()) {
                    E result = (E) classData.newInstance();
                    for (FieldMetadata field : fields) {
                        field.setValue(result, resultSet.getObject(field.getColumnName()));
                    }
                    results.add(result);
                }
            }
        }
        return results;
    }

    public void setPk(String id) throws ReflectiveOperationException {
        this.pk.setValueIn(this, id);
    }

    @Override
    public void validateCreate(Connection connection) throws Exception {

    }

    @Override
    public void validateUpdate(Connection connection) throws Exception {

    }

    @Override
    public void validateDelete(Connection connection) throws Exception {

    }
}
