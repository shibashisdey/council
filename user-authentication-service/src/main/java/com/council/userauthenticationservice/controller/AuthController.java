package com.council.userauthenticationservice.controller;


import com.council.userauthenticationservice.dto.UserInternalResponse;
import com.council.userauthenticationservice.exception.UserAlreadyExistsException;
import com.council.userauthenticationservice.model.LoginRequest;
import com.council.userauthenticationservice.model.RegisterRequest;
import com.council.userauthenticationservice.service.AuthService;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/auth")
public class AuthController {

    private final AuthService service;

    public AuthController(AuthService service) {
        this.service = service;
    }

    @PostMapping("/login")
    public Map<String, String> login(@RequestBody LoginRequest request) {
        return Map.of("token", service.login(request));
    }

    @PostMapping("/register")
    public Map<String, String> register(@RequestBody RegisterRequest request) throws UserAlreadyExistsException {
        service.register(request);
        return Map.of("message", "User registered successfully");
    }

    @GetMapping("/users/{userId}/internal")
    public UserInternalResponse getUserInternal(@PathVariable Long userId) {
        return service.getUserInternal(userId);
    }
}
