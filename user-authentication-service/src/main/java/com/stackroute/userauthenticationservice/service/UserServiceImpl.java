package com.stackroute.userauthenticationservice.service;

import com.stackroute.userauthenticationservice.Repository.UserRepository;
import com.stackroute.userauthenticationservice.exception.IncorrectPasswordException;
import com.stackroute.userauthenticationservice.exception.UserExistsException;
import com.stackroute.userauthenticationservice.exception.UserNotFoundException;
import com.stackroute.userauthenticationservice.model.User;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
public class UserServiceImpl implements UserService{
    @Autowired
    private UserRepository repository;
    @Override
    public User registerUser(User user) throws UserExistsException {
        final boolean existById = this.repository.existsById(user.getEmail());
        if(existById){
            throw new UserExistsException("User already Exist");
        }
        return this.repository.save(user);
    }

    @Override
    public User authenticateUser(String email, String password) throws UserNotFoundException, IncorrectPasswordException {
        final boolean existById = this.repository.existsById(email);
        if(!existById){
            throw new UserNotFoundException("User Does not exist with the given email");
        }
        final Optional<User> optUser = this.repository.findByEmailAndPassword(email,password);
        if(optUser.isEmpty()){
            throw new IncorrectPasswordException("Password Mismatch");
        }
        return optUser.get();
    }
}

