package com.council.userauthenticationservice.exception;

public class IncorrectPasswordException extends Exception {
    public IncorrectPasswordException(String passwordMismatch) {
        super(passwordMismatch);
    }
}
