package com.udframework.database.exceptions;

public class UnauthorizedClass extends Unauthorized{
    public UnauthorizedClass() {
    }

    public UnauthorizedClass(String message) {
        super(message);
    }

    public UnauthorizedClass(String message, Throwable cause) {
        super(message, cause);
    }
}
