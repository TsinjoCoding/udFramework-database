package com.udframework.database.generic.family;

import com.udframework.database.DBObject;
import com.udframework.database.exceptions.DatabaseException;

public abstract class Child <E extends Mother> extends DBObject {

    public Child() throws DatabaseException, NoSuchMethodException {
    }

    public abstract E getMother ();
    public abstract void setMother (E mother);

}
