package com.udframework.database.generic.family;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;

import com.udframework.database.DBObject;
import com.udframework.database.exceptions.DatabaseException;

public abstract class Mother<E extends Child> extends DBObject {

    public Mother() throws DatabaseException, NoSuchMethodException {
    }

    public abstract List<E> getChildren();
    public abstract void setChildren(List<E> children);

    public List<E> getChildren(Connection connection) throws ReflectiveOperationException, SQLException, DatabaseException {
        if (getChildren() == null) {
            this.populateField("children", connection);
        }
        return getChildren();
    }


    @Override
    public void create(Connection connection) throws Exception {
        try {
            connection.setAutoCommit(false);
            createSelf(connection);
            connection.commit();
        } catch (Exception e) {
            connection.rollback();
            throw e;
        }
    }

    protected void createSelf (Connection connection) throws Exception {
        super.create(connection);
        createChildren(connection);
    }

    protected void createChildren(Connection connection) throws Exception {
        if (getChildren() != null) {
            for (E child : getChildren()) {
                child.setMother(this);
                child.create(connection);
            }
        }
    }

    public void createWithoutTransaction (Connection connection) throws Exception {
        createSelf(connection);
    }

    public void populateChildren(Connection connection) throws DatabaseException, Exception {
        this.populateField("children", connection);
    }

}
