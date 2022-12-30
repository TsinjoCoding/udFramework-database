package com.udframework.database;

import com.udframework.database.connectionHandler.ConnectionHandler;
import com.udframework.database.exceptions.DatabaseException;
import com.udframework.database.generic.ClassMetadata;
import com.udframework.database.generic.FKField;
import com.udframework.database.generic.FieldMetadata;

import java.sql.Connection;
import java.util.ArrayList;
import java.util.Collections;

public class DBInherit<E extends DBInherit<E,P>,P> extends DBObject<E, P>{

    private final ClassMetadata superData;
    private final String fullSelect;
    private FieldMetadata[] joinedFields;
    private final boolean active;

    public DBInherit() throws DatabaseException, NoSuchMethodException {
        Class<?> superclass = getClass().getSuperclass();
        this.active = superclass != DBInherit.class;
        superData = this.active ? ClassMetadata.getMetadataOf(superclass) : null;
        this.fullSelect = buildFullSelect();
        setJoinedFields();
    }

    private void setJoinedFields() {
        if (active) {
            ArrayList<FieldMetadata> joined = new ArrayList<>();
            Collections.addAll(joined, classData.getListFields().toArray(new FieldMetadata[0]));
            for (FieldMetadata field : superData.getListFields()) {
                if (field instanceof FKField) continue;
                joined.add(field);
            }
            this.joinedFields = joined.toArray(new FieldMetadata[0]);
        }
    }

    private String buildFullSelect() {
        if (active) {
            String columns = "\"" + classData.getTableName() + "\".\"" + classData.getPrimaryKey().getColumnName() + "\", " + classData.columnsWithoutPK() + ", " + superData.columnsWithoutPK();
            String table = classData.getTableName() + " join " + superData.getTableName() +
                    " on " + classData.getTableName() + "." + classData.getFk().getColumnName() +
                    " = " + superData.getTableName() + "." + superData.getPrimaryKey().getColumnName();
            return String.format(defaultSelect, columns, table);
        }
        return this.instanceSelect;
    }

    @Override
    public void create(Connection connection) throws Exception {
        if (superData.getPkValue(this) == null) {
            try {
                connection.setAutoCommit(false);
                super.create(connection, this.superData);
                super.create(connection);
                connection.commit();
            } catch (Exception e) {
                connection.rollback();
                throw e;
            }
            return;
        }
        super.create(connection);
    }

    public void delete(boolean both) throws Exception {
        try(Connection connection = ConnectionHandler.createSession()) {
            delete(connection, both);
        }
    }

    public void delete (Connection connection, boolean both) throws Exception {
        if (both) {
            try {
                connection.setAutoCommit(false);
                delete(connection);
                delete(connection, this.superData);
                connection.commit();
            }
            catch (Exception e) {
                connection.rollback();
                throw e;
            }
            return;
        }
        delete(connection);
    }

    public Temp<E> fullFetch() throws Exception {
        Connection connection = ConnectionHandler.createSession();
        Temp<E> temp = fullFetch(connection);
        temp.closeAfterRun();
        return temp;
    }

    public Temp<E> fullFetch (Connection connection) {
        return new TempJoin<E>(fullSelect, connection, (E) this, joinedFields);
    }

    public void updateSuper () throws Exception {
        try (Connection connection = ConnectionHandler.createSession()) {
            updateSuper(connection);
        }
    }

    public void updateSuper(Connection connection) throws Exception {
        super.update(connection, this.superData);
    }

    public void update (boolean both) throws Exception {
        try (Connection connection = ConnectionHandler.createSession()) {
            update(connection, both);
        }
    }

    public void update(Connection connection, boolean both) throws Exception {
        if (both) {
            try {
                connection.setAutoCommit(false);
                updateSuper(connection);
                update(connection);
                connection.commit();
            }
            catch (Exception e) {
                connection.rollback();
                throw e;
            }
            return;
        }
        update(connection);
    }

}
