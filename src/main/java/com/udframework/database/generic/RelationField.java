package com.udframework.database.generic;

import java.lang.reflect.Field;
import java.util.List;

import com.udframework.database.exceptions.DatabaseException;
import com.udframework.database.exceptions.IgnoredException;
import com.udframework.database.generic.common.DBField;

public class RelationField extends DBField {



    public RelationField(Field field) throws NoSuchMethodException, IgnoredException, DatabaseException {
        super(field);
        if (relationType == null) throw new IgnoredException ();
        extractMethods();
        if ( !relationType.isOne() && !getType().isAssignableFrom(List.class)) {
            throw new DatabaseException(String.format("relation type 'MANY' requires %s object", List.class.getName()));
        }
    }

    @Override
    protected void extractRelations () {
        if (check1T1()) return;
        else if (check1TM()) return;
        checkMTM();
    }

}
