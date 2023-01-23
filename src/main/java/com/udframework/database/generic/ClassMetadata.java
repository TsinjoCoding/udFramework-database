package com.udframework.database.generic;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;

import com.udframework.database.DBObject;
import com.udframework.database.annotations.*;
import com.udframework.database.exceptions.DatabaseException;
import com.udframework.database.exceptions.IgnoredException;
import com.udframework.database.generic.utils.StringUtils;

public class ClassMetadata {

    private final Class<?> clazz;
    private String tableName;
    private final HashMap<String, FieldMetadata> fields = new HashMap<>();
    private final ArrayList<FieldMetadata> listFields = new ArrayList<>();
    public final HashMap<String, RelationField> relations = new HashMap<>();
    private FieldMetadata primaryKey, fk;
    private static final HashMap<Class<?>, ClassMetadata> stored = new HashMap<>();
    private String allColumns;
    private String deleteTable;
    private String deleteColumn;
    private boolean passField = false;
    private DBSequence pkSequence;

    public FieldMetadata getFk() {
        return fk;
    }

    public String getDeleteColumn() {
        return deleteColumn;
    }

    public FieldMetadata getPrimaryKey() {
        return primaryKey;
    }

    public DBSequence getPkSequence() {
        return pkSequence;
    }

    public Object newInstance() throws ReflectiveOperationException {
        try {
            return this.clazz.getConstructor().newInstance();
        } catch (NoSuchMethodException e) {
            throw new NoSuchMethodException("constructor with no params does not exist");
        }
    }

    public static ClassMetadata getMetadataOf(Class<?> toGet) throws DatabaseException, NoSuchMethodException {

        if (stored.containsKey(toGet)) {
            return stored.get(toGet);
        }

        ClassMetadata meta = new ClassMetadata(toGet);
        stored.put(toGet, meta);
        meta.registerSequence();

        return meta;
    }

    private ClassMetadata(Class<?> clazz) throws DatabaseException, NoSuchMethodException {
        this.clazz = clazz;
        extract();
        setAllColumns();
    }

    private void setAllColumns() {
        StringBuilder column = new StringBuilder();
        String[] columns = getColumnNames();
        column.append(StringUtils.wrap(columns[0]));

        for (int i = 1; i < columns.length; i++) {
            column.append(" , ").append(StringUtils.wrap(columns[i]));
        }

        this.allColumns = String.valueOf(column);
    }

    public String columnsWithoutPK() {
        return columnsWithout(primaryKey.columnName);
    }

    public String getAllColumns() {
        return allColumns;
    }

    public String getAllValues(Object obj) throws ReflectiveOperationException, DatabaseException {
        String[] columns = getColumnNames();
        StringBuilder values = new StringBuilder();

        values.append(valueToString(getFieldMetadata(columns[0]).valueIn(obj)));

        for (int i = 1; i < columns.length; i++) {
            values.append(" , ").append(valueToString(getFieldMetadata(columns[i]).valueIn(obj)));
        }

        return values.toString();
    }

    public static String valueToString(Object object) {
        if (object == null)
            return "null";
        return String.format("'%s'", object);
    }

    private void extract() throws DatabaseException, NoSuchMethodException {
        setPassField();
        extractTableName();
        extractColumns();
        setGeneratedValue();
    }

    private void setGeneratedValue() throws DatabaseException {
        GeneratedValue generatedValue = clazz.getAnnotation(GeneratedValue.class);
        if (generatedValue != null) {
            if (this.primaryKey == null) throw new DatabaseException("primary key is not present");
            this.primaryKey.setGeneratedValueInfo(generatedValue);
            this.primaryKey.setSequence(this.pkSequence);
        }
    }

    private void registerSequence() throws DatabaseException, NoSuchMethodException {
        this.pkSequence = DBSequence.register(clazz);
        setGeneratedValue();
    }

    private void setPassField() {
        passField = clazz.isAnnotationPresent(PassField.class);
    }

    private void extractColumns() throws DatabaseException, NoSuchMethodException {
        Field[] declaredFields = extractAllowedFields();
        GeneratedField[] generatedFields = extractGeneratedFields();

        for (Field field : declaredFields) {
            try {
                FieldMetadata fieldMetadata = new FieldMetadata(field);
                setGenerated(fieldMetadata, generatedFields);
                
                if (fieldMetadata.isPrimaryKey()) {
                    if (primaryKey != null) throw new DatabaseException("multiple primary key are not allowed");
                    primaryKey = fieldMetadata;
                }

                String key = fieldMetadata.getColumnName();
                if (fields.containsKey(key))
                    throw new DatabaseException("column %s should be unique");
                addField(fieldMetadata);
            }
            catch (IgnoredException ignored1) {
                try {
                    RelationField relationField = new RelationField(field);
                    relations.put(relationField.fieldName().toLowerCase(), relationField);
                } catch (IgnoredException ignored2) {}
            }
        }
        extractFKField();
    }

    private void addField(FieldMetadata fieldMetadata) {
        listFields.add(fieldMetadata);
        fields.put(fieldMetadata.getColumnName(), fieldMetadata);
    }

    public ArrayList<FieldMetadata> getListFields() {
        return listFields;
    }

    private void extractFKField() throws DatabaseException, NoSuchMethodException {
        if (this.clazz.isAnnotationPresent(TableInheritance.class)) {
            ClassMetadata superData = ClassMetadata.getMetadataOf(this.clazz.getSuperclass());
            try {
                String fkName = getFkName();
                FKField fkField = new FKField(superData.primaryKey.getField(), fkName);
                this.fk = fkField;
                addField(fkField);
            } catch (IgnoredException e) {
                throw new RuntimeException(e);
            }
        }
    }

    private String getFkName() {
        SuperPK superPK = this.clazz.getAnnotation(SuperPK.class);
        if (superPK != null) {
            return superPK.value();
        }
        else {
            return this.clazz.getSuperclass().getSimpleName().toLowerCase() + "Id";
        }
    }

    private void setGenerated(FieldMetadata fieldMetadata, GeneratedField[] generatedFields) {
        if(generatedFields == null) return;
        for (GeneratedField generatedField : generatedFields) {
            if (generatedField.field().equals(fieldMetadata.fieldName())) {
                fieldMetadata.setGeneratedValueInfo(generatedField.auto());
                return;
            }
        }
    }

    private GeneratedField[] extractGeneratedFields() {
        Generated generated = clazz.getAnnotation(Generated.class);
        if (generated == null) return null;
        return generated.value();
    }

    private Field[] extractAllowedFields() {
        ArrayList<Field> allowedFields = new ArrayList<>();
        Collections.addAll(allowedFields, clazz.getDeclaredFields());
        Class<?> parent = clazz.getSuperclass();
        while (DBObject.class.isAssignableFrom(parent)) {
            if (parent.isAnnotationPresent(PassField.class)) {
                Collections.addAll(allowedFields, parent.getDeclaredFields());
            }
            parent = parent.getSuperclass();
        }
        return allowedFields.toArray(new Field[0]);
    }

    private void extractTableName() throws DatabaseException {
        DBTable dbTable = this.clazz.getAnnotation(DBTable.class);
        this.tableName = dbTable.tableName().isEmpty() ? this.clazz.getSimpleName() : dbTable.tableName();

        DBDelete dbDelete = this.clazz.getAnnotation(DBDelete.class);
        this.deleteTable = dbDelete == null ? "" : dbDelete.tableName();
        this.deleteColumn = dbDelete == null ? "" : dbDelete.columnName();

        if (!deleteTable.isEmpty() && deleteColumn.isEmpty()) {
            throw new DatabaseException("deleteColumn cannot be empty if deleteTable is not");
        }
        this.tableName = this.tableName.toLowerCase();
    }

    public String getDeleteTable() {
        return deleteTable;
    }

    public String getTableName() {
        return tableName;
    }

    public HashMap<String, FieldMetadata> getFields() {
        return fields;
    }

    public String[] getColumnNames() {
        return getFields().keySet().toArray(new String[0]);
    }

    public FieldMetadata getFieldMetadata(String column) {
        return fields.get(column);
    }

    public Object getPkValue (Object obj) throws ReflectiveOperationException {
        return primaryKey.getValueIn(obj);
    }

    public String getAllColumnsPrepared() {
        String str = "";
        for(String col: getColumnNames()) {
            str = str + "? ,";
        }
        str = str.substring(0, str.length() - 2);
        return str;
    }

    public String columnsWithoutFK() {
       return columnsWithout(fk.getColumnName());
    }

    private String columnsWithout(String col) {
        StringBuilder column = new StringBuilder();
        String[] columns = getColumnNames();

        for (String s : columns) {
            if (col.compareTo(s) == 0) {
                continue;
            }
            column.append(" , ").append(StringUtils.wrap(s));
        }

        return String.valueOf(column.substring(2));
    }

}
