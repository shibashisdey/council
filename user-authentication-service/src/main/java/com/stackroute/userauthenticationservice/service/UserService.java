package com.stackroute.userauthenticationservice.service;
import com.stackroute.userauthenticationservice.exception.IncorrectPasswordException;
import com.stackroute.userauthenticationservice.exception.UserExistsException;
import com.stackroute.userauthenticationservice.exception.UserNotFoundException;
import com.stackroute.userauthenticationservice.model.User;
public interface UserService {
    User registerUser(User user) throws UserExistsException;
    User authenticateUser(String email,String password) throws UserNotFoundException, IncorrectPasswordException;
}
