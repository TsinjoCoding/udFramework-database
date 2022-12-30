package com.udframework;

import com.udframework.bdd.connectionHandler.ConnectionHandler;

public class Main {
    public static void main(String[] args) throws Exception{
        ConnectionHandler.getInstance();
    }
}