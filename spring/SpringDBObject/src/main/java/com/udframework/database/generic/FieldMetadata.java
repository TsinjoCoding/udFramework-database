package com.udframework.database.generic;

import java.lang.reflect.Field;
import java.sql.Connection;
import java.sql.SQLException;

import com.udframework.database.DBObject;
import com.udframework.database.annotations.*;
import com.udframework.database.exceptions.DatabaseException;
import com.udframework.database.exceptions.IgnoredException;
import com.udframework.database.exceptions.UnauthorizedType;
import com.udframework.database.generic.common.DBField;
import com.udframework.database.generic.utils.StringUtils;

public class FieldMetadata extends DBField {

    private boolean primaryKey;
    protected String columnName;
    private GeneratedValue generatedValueInfo;
    private String joinTo;
    private DBSequence seq;

    public FieldMetadata(Field field) throws UnauthorizedType, IgnoredException, NoSuchMethodException {
        super(field);
        extract();
    }

    private FieldMetadata(Field field, int a) throws NoSuchMethodException {
        super(field);
    }

    public boolean isGenerated() {
        return generatedValueInfo != null;
    }

    public boolean isGeneratedString() {
        if (!isGenerated())
            return false;
        return !generatedValueInfo.completedStr().isEmpty();
    }

    private void extract() throws UnauthorizedType, IgnoredException, NoSuchMethodException {
        extractColumnName();
        if (field.getType().isPrimitive()) {
            throw new UnauthorizedType(String.format("type of %s is primitive", this.columnName));
        }
        extractMethods();
        registerSequence();
        extractGeneratedInfo();
        extractRelations();
    }

    protected void registerSequence() {
        Sequence seq = this.field.getAnnotation(Sequence.class);
        if(seq == null) return;
        DBSequence.register(seq);
    }

    @Override
    protected void extractRelations() {
        if (check1T1())
            return;
        checkMT1();
    }

    @Override
    protected boolean check1T1() {
        boolean b = super.check1T1();
        if (b) {
            this.joinTo = ((OneToOne) this.relationType.getAnnotation()).joinTo();
        }
        return b;
    }

    @Override
    protected boolean checkMT1() {
        boolean b = super.checkMT1();
        if (b) {
            this.joinTo = ((ManyToOne) this.relationType.getAnnotation()).joinTo();
        }
        return b;
    }

    protected void extractGeneratedInfo() {
        GeneratedValue g = this.field.getAnnotation(GeneratedValue.class);
        if (g == null) return;
        setGeneratedValueInfo(g);
        setSequence(DBSequence.getDBSequence(field.getName()));
    }

    public void setSequence(DBSequence dbSequence) {
        this.seq = dbSequence;
    }


    void setGeneratedValueInfo(GeneratedValue generatedValueInfo) {
        this.generatedValueInfo = generatedValueInfo;
    }

    protected void extractColumnName() throws IgnoredException, NoSuchMethodException {
        DBColumn dbColumn = this.field.getAnnotation(DBColumn.class);

        if (dbColumn == null)
            throw new IgnoredException();

        this.primaryKey = dbColumn.isPrimaryKey();
        this.columnName = (dbColumn.columnName().isEmpty()) ? this.field.getName() : dbColumn.columnName();
        this.columnName = this.columnName.toLowerCase();
    }

    public String getColumnName() {
        return columnName;
    }

    public boolean isPrimaryKey() {
        return primaryKey;
    }

    public Object valueIn(Object obj) throws ReflectiveOperationException, DatabaseException {
        Object value = getValueIn(obj);
        if (isInRelation()) {
            return inRelationValue(value);
        }
        return value;
    }

    private Object inRelationValue(Object value) throws DatabaseException, ReflectiveOperationException {
        if (!isInRelation() || value == null)
            return null;
        ClassMetadata classMetadata = ClassMetadata.getMetadataOf(value.getClass());
        return classMetadata.getFieldMetadata(this.joinTo).valueIn(value);
    }

    public void setValue(Object obj, Object value) throws ReflectiveOperationException, DatabaseException {
        if (isInRelation() && value != null) {
            value = getRelationValue(obj, value);
        }
        setValueIn(obj, value);
    }

    @Override
    public void setValueIn(Object obj, Object val) throws ReflectiveOperationException {
        if (isPrimaryKey() && val != null) {
            if (getType() == Long.class ) {
                val = Long.parseLong(val.toString());
            }
            else if (getType() == Integer.class) {
                val = Integer.parseInt(val.toString());
            }
            else if (getType() == String.class) {
                val = val.toString();
            }
        }
        super.setValueIn(obj, val);
    }

    private Object getRelationValue(Object obj, Object value) throws ReflectiveOperationException, DatabaseException {
        Object toSet = getType().getConstructor().newInstance();
        ClassMetadata classData = ClassMetadata.getMetadataOf(getType());
        classData.getFieldMetadata(joinTo).setValue(toSet, value);
        return toSet;
    }

    public Object buildGeneratedValue(Long seq_val) throws DatabaseException {
        if (!isGenerated()) throw new DatabaseException(columnName + " cant be generated");
        if (!isGeneratedString()) {
            if (this.getType() == Integer.class) return seq_val.intValue();
            return seq_val;
        }
        if (this.generatedValueInfo.len() == Integer.MAX_VALUE) {
            throw new DatabaseException("please set the length of the generated value for the column: " + columnName);
        }
        String str_seq = String.valueOf(seq_val);

        int len = this.generatedValueInfo.len() - str_seq.length() - this.generatedValueInfo.completedStr().length();
        len++;

        if (this.generatedValueInfo.lPad()) {
            return this.generatedValueInfo.completedStr() + StringUtils.lPad(len, str_seq, this.generatedValueInfo.padChar());
        }
        return StringUtils.rPad(len, str_seq, this.generatedValueInfo.padChar()) + this.generatedValueInfo.completedStr();
    }

    public void setDefaultIn(DBObject<?, ?> obj, ClassMetadata classData, Connection connection) throws SQLException, DatabaseException, ReflectiveOperationException {
        if (!isGenerated()) return;
        Class<?> valType = isGeneratedString() ? String.class : Long.class;
        Object generatedValue = obj.nextGeneratedValue(columnName, connection);
        setValue(obj, generatedValue);
    }

    public DBSequence sequence() {
        return seq;
    }
}
