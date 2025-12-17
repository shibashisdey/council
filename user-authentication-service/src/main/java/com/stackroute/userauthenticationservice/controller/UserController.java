package com.stackroute.userauthenticationservice.controller;

import com.stackroute.userauthenticationservice.exception.IncorrectPasswordException;
import com.stackroute.userauthenticationservice.exception.UserExistsException;
import com.stackroute.userauthenticationservice.exception.UserNotFoundException;
import com.stackroute.userauthenticationservice.model.User;
import com.stackroute.userauthenticationservice.model.UserCredential;
import com.stackroute.userauthenticationservice.service.TokenGeneratorService;
import com.stackroute.userauthenticationservice.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("api/v1/users")
public class UserController {
    @Autowired
    private UserService userService;

    @Autowired
    private TokenGeneratorService tokenGeneratorService;


    ResponseEntity<?> responseEntity;

    @PostMapping("register")
    public User registerUser(@RequestBody User user) throws UserExistsException {
        return this.userService.registerUser(user);
    }
//    @PostMapping("login")
//    public Map<String,String> authenticateUser(@RequestBody UserCredential credential) throws UserNotFoundException, IncorrectPasswordException {
//        final User user = this.userService.authenticateUser(credential.getEmail(), credential.getPassword());
//        return this.tokenGeneratorService.generateToken(credential);
//    }

    @PostMapping("login")
    public ResponseEntity<?> authenticateUser(@RequestBody UserCredential credential) throws UserNotFoundException, IncorrectPasswordException {
        System.out.println("Successful");
        final User user = this.userService.authenticateUser(credential.getEmail(), credential.getPassword());
        ;
        return new ResponseEntity<>(tokenGeneratorService.generateToken(credential), HttpStatus.OK);

    }


}

