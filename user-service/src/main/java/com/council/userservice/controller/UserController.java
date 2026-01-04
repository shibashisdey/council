package com.council.userservice.controller;

import com.council.userservice.dto.request.CreateUserRequest;
import com.council.userservice.dto.request.UpdateUserRequest;
import com.council.userservice.dto.response.UserResponse;
import com.council.userservice.service.UserService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/users")
public class UserController {

    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    /**
     * Create user profile (called after login)
     */
    @PostMapping
    public ResponseEntity<UserResponse> createUser(
            @RequestHeader("X-USER-ID") Long userId,
            @RequestBody CreateUserRequest request
    ) {
        return new ResponseEntity<>(
                userService.createUser(userId, request),
                HttpStatus.CREATED
        );
    }

    /**
     * Get own user profile
     */
    @GetMapping("/me")
    public ResponseEntity<UserResponse> getMyProfile(
            @RequestHeader("X-USER-ID") Long userId
    ) {
        return ResponseEntity.ok(userService.getUserById(userId));
    }

    @PatchMapping("/me")
    public UserResponse updateMe(
            @RequestHeader("X-USER-ID") Long userId,
            @RequestBody UpdateUserRequest request
    ) {
        return userService.updateUser(userId, request);
    }
}
