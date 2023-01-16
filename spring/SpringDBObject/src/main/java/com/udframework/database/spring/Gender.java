package com.udframework.database.spring;


import com.udframework.database.annotations.DBTable;
import com.udframework.database.common.HasName;
import com.udframework.database.exceptions.DatabaseException;

@DBTable
public class Gender extends HasName<Gender> {
    public Gender() throws DatabaseException, NoSuchMethodException {
    }
}
