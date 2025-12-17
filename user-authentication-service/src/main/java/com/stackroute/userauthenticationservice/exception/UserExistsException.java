package com.stackroute.userauthenticationservice.exception;

public class UserExistsException extends Exception {
    public UserExistsException(String userAlreadyExist) {
        super(userAlreadyExist);
    }
}
