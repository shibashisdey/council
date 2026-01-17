package com.council.userservice.controller;

import com.council.userservice.dto.request.CreateUserRequest;
import com.council.userservice.dto.request.UpdateUserRequest;
import com.council.userservice.dto.response.UserResponse;
import com.council.userservice.service.UserService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;


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
            @RequestHeader("X-USER-EMAIL") String email,
            @RequestBody CreateUserRequest request
    ) {
        return new ResponseEntity<>(
                userService.createUser(userId, email, request),
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

    /**
     * Update own user profile (partial update)
     */
    @PatchMapping("/me")
    public ResponseEntity<UserResponse> updateMe(
            @RequestHeader("X-USER-ID") Long userId,
            @RequestBody UpdateUserRequest request
    ) {
        return ResponseEntity.ok(userService.updateUser(userId, request));
    }

    /**
     * Get user profile by ID (internal use)
     */
    @GetMapping("/{id}/public")
    public ResponseEntity<UserResponse> getPublicUserProfile(
            @PathVariable Long id,
            @RequestHeader(value = "X-INTERNAL-CALL", required = false) String internal
    ) {
        if (!"true".equals(internal)) {
            throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN,
                    "Internal access only"
            );
        }

        return ResponseEntity.ok(userService.getPublicUserById(id));
    }
}
