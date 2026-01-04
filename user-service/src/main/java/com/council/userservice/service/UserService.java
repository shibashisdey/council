package com.council.userservice.service;

import com.council.userservice.dto.request.CreateUserRequest;
import com.council.userservice.dto.request.UpdateUserRequest;
import com.council.userservice.dto.response.UserResponse;

public interface UserService {

    UserResponse getUserById(Long userId);

    UserResponse getUserByEmail(String email);

    UserResponse createUser(Long userId, CreateUserRequest request);

    UserResponse updateUser(Long userId, UpdateUserRequest request);

}
