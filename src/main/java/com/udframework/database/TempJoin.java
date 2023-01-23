package com.udframework.database;

import com.udframework.database.exceptions.DatabaseException;
import com.udframework.database.generic.FieldMetadata;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.ArrayList;

public class TempJoin<E extends DBInherit> extends Temp<E> {

    public FieldMetadata[] fields;

    public TempJoin(String defaultSelect, Connection connection, E obj, FieldMetadata[] fields) {
        super(defaultSelect, connection, obj);
        this.fields = fields;
    }

    @Override
    public ArrayList<E> run() throws ReflectiveOperationException, SQLException, DatabaseException {
        try {
            return obj.runQuery(buildQuery(), getValues(),  connection, fields);
        }
        finally {
            if (closeConnection) {
                connection.close();
            }
        }
    }

}
