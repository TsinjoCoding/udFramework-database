package com.udframework.bdd.exceptions;

public class UnauthorizedType extends Unauthorized {
    public UnauthorizedType() {
    }

    public UnauthorizedType(String message) {
        super(message);
    }

    public UnauthorizedType(String message, Throwable cause) {
        super(message, cause);
    }
}
