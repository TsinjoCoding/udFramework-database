package com.udframework.bdd.generic.family;

import com.udframework.bdd.DBObject;
import com.udframework.bdd.exceptions.DatabaseException;

public abstract class Child <E extends Mother> extends DBObject {

    public Child() throws DatabaseException, NoSuchMethodException {
    }

    public abstract E getMother ();
    public abstract void setMother (E mother);

}
