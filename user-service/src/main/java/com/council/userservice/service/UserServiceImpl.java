package com.council.userservice.service;

import com.council.userservice.dto.request.CreateUserRequest;
import com.council.userservice.dto.request.UpdateUserRequest;
import com.council.userservice.dto.response.UserResponse;
import com.council.userservice.model.User;
import com.council.userservice.repository.UserRepository;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.Period;

@Service
public class UserServiceImpl implements UserService {

    private final UserRepository userRepository;

    public UserServiceImpl(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Override
    public UserResponse getUserById(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));

        return mapToResponse(user);
    }

    @Override
    public UserResponse getUserByEmail(String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));

        return mapToResponse(user);
    }

    private UserResponse mapToResponse(User user) {

        Integer age = null;
        if (user.getDateOfBirth() != null) {
            age = Period
                    .between(user.getDateOfBirth(), LocalDate.now())
                    .getYears();
        }

        return UserResponse.builder()
                .userId(user.getId())          // ✅ matches DTO
                .fullName(user.getFullName())
                .age(age)                      // ✅ derived, not stored
                .gender(user.getGender())
                .city(null)                    // ✅ placeholder (or remove field)
                .build();
    }
    @Override
    public UserResponse createUser(Long userId, CreateUserRequest request) {

        if (userRepository.existsById(userId)) {
            throw new IllegalStateException("User profile already exists");
        }

        User user = new User();
        user.setId(userId);
        user.setFullName(request.getFullName());
        user.setPhoneNumber(request.getPhoneNumber());
        user.setGender(request.getGender());
        user.setDateOfBirth(request.getDateOfBirth());

        User saved = userRepository.save(user);
        return mapToResponse(saved);
    }

    @Override
    public UserResponse updateUser(Long userId, UpdateUserRequest request) {

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));

        // PATCH-style updates
        if (request.getFullName() != null) {
            user.setFullName(request.getFullName());
        }

        if (request.getEmail() != null) {
            user.setEmail(request.getEmail());
        }

        if (request.getPhoneNumber() != null) {
            user.setPhoneNumber(request.getPhoneNumber());
        }

        if (request.getGender() != null) {
            user.setGender(request.getGender());
        }

        if (request.getDateOfBirth() != null) {
            user.setDateOfBirth(request.getDateOfBirth());
        }

        if (request.getCity() != null) {
            user.setCity(request.getCity());
        }

        User updated = userRepository.save(user);
        return mapToResponse(updated);
    }

}
