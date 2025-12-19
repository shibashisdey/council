package com.council.userauthenticationservice.exception;

public class UserAlreadyExistsException extends Exception {
    public UserAlreadyExistsException(String userAlreadyExist) {
        super(userAlreadyExist);
    }
}
