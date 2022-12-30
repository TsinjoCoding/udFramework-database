package com.udframework.bdd.exceptions;

public class Unauthorized extends DatabaseException{
    public Unauthorized() {
    }

    public Unauthorized(String message) {
        super(message);
    }

    public Unauthorized(String message, Throwable cause) {
        super(message, cause);
    }
}
