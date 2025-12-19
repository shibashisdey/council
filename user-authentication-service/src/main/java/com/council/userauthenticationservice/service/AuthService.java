package com.council.userauthenticationservice.service;

import com.council.userauthenticationservice.exception.UserAlreadyExistsException;
import com.council.userauthenticationservice.model.LoginRequest;
import com.council.userauthenticationservice.model.RegisterRequest;

public interface AuthService {
    String login(LoginRequest request);

    void register(RegisterRequest request) throws UserAlreadyExistsException;
}
