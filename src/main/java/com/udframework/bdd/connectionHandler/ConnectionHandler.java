package com.udframework.bdd.connectionHandler;

import com.udframework.bdd.ConnectionGetter;
import com.udframework.bdd.annotations.ConnectionProvider;
import com.udframework.bdd.util.ClassUtil;

import java.sql.Connection;
import java.sql.SQLException;

public class ConnectionHandler {

    private ConnectionGetter connectionGetter;
    private static ConnectionHandler instance;
    private Class<? extends ConnectionGetter> clazz;

    public static ConnectionHandler getInstance () throws ReflectiveOperationException {
        if (instance == null) {
            instance = new ConnectionHandler();
        }
        return instance;
    }

    public static Connection createSession () throws SQLException, ReflectiveOperationException {
        return getInstance().connectionGetter.getConnection();
    }

    public ConnectionHandler() throws ReflectiveOperationException {
        extract();
    }

    private void extract() throws ReflectiveOperationException {
        findClass();
        if (clazz == null) throw new ClassNotFoundException("No connectionProvider found");
        this.connectionGetter = (ConnectionGetter) ClassUtil.createInstance(clazz);
    }

    private int isIt (Class<?> clazz) {
        if (clazz.isAnnotationPresent(ConnectionProvider.class)) return 0;
        return -1;
    }

    private void findClass() throws ClassNotFoundException {
        try {
            this.clazz = (Class<? extends ConnectionGetter>) ClassUtil.findClassOnClassPath(this::isIt, "");
        }
        catch (ClassNotFoundException e) {
            throw new ClassNotFoundException("Connection provider was not found");
        }
        catch (ClassCastException e) {
            throw new ClassCastException("Connection provider must implement ConnectionGetter interface");
        }
    }

}
