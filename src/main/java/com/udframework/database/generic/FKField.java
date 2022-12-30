package com.udframework.database.generic;

import com.udframework.database.exceptions.IgnoredException;
import com.udframework.database.exceptions.UnauthorizedType;

import java.lang.reflect.Field;

public class FKField extends FieldMetadata{

    public FKField(Field field, String name) throws UnauthorizedType, IgnoredException, NoSuchMethodException {
        super(field);
        this.columnName = name.toLowerCase();
    }


    @Override
    protected void extractColumnName() throws IgnoredException, NoSuchMethodException {}

    @Override
    protected void registerSequence() {}

    @Override
    protected void extractGeneratedInfo() {}

    @Override
    protected void extractRelations() {}
}
