package com.udframework.bdd.generic.common;

import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.lang.reflect.Method;

public class FieldData {

    protected final Field field;
    protected String firstUpperCase;
    private Method getter, setter;

    public FieldData(Field field) {
        this.field = field;
        init();
    }

    protected void init() {
        setFirstUpperCase();
    }

    protected void extractMethods() throws NoSuchMethodException {
        extractGetter();
        extractSetter();
    }

    private void extractSetter() throws NoSuchMethodException {
        try {
            this.setter = field.getDeclaringClass().getMethod("set" +  this.firstUpperCase, getType());
        } catch (NoSuchMethodException e) {
            throw new NoSuchMethodException("no setter for the field " + fieldName());
        }
    }

    private void extractGetter() throws NoSuchMethodException {
        try {
            this.getter = field.getDeclaringClass().getMethod("get"+this.firstUpperCase);
        } catch (NoSuchMethodException e) {
            throw new NoSuchMethodException("no getter for the field " + fieldName());
        }
    }

    public String fieldName() {
        return this.field.getName();
    }

    public Class<?> getType () {
        return field.getType();
    }

    protected void setFirstUpperCase() {
        String name = this.field.getName();
        char firstLetter = (name.toCharArray()[0]+"").toUpperCase().toCharArray()[0];
        this.firstUpperCase = firstLetter + name.substring(1);
    }

    public Object getValueIn(Object obj) throws ReflectiveOperationException {
        return getter.invoke(obj);
    }

    public void setValueIn(Object obj, Object val) throws ReflectiveOperationException {
        try {
            setter.invoke(obj, val);
        }
        catch (Throwable e) {
            throw e;
        }
    }

    public <T extends Annotation> T getAnnotation(Class<T> aClass) {
        return this.field.getAnnotation(aClass);
    }

    public Field getField() {
        return field;
    }

}
