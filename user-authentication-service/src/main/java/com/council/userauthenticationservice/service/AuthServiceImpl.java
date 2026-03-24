package com.council.userauthenticationservice.service;

import com.council.userauthenticationservice.Repository.UserRepository;
import com.council.userauthenticationservice.dto.UserInternalResponse;
import com.council.userauthenticationservice.exception.InvalidCredentialsException;
import com.council.userauthenticationservice.exception.UserAlreadyExistsException;
import com.council.userauthenticationservice.exception.UserNotFoundException;
import com.council.userauthenticationservice.messaging.EmailEventPublisher;
import com.council.userauthenticationservice.messaging.EmailNotificationEvent;
import com.council.userauthenticationservice.model.LoginRequest;
import com.council.userauthenticationservice.model.RegisterRequest;
import com.council.userauthenticationservice.model.User;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.Instant;

@Service
public class AuthServiceImpl implements AuthService {

    private final UserRepository repository;
    private final PasswordEncoder encoder;
    private final JwtService jwtService;
    private final EmailEventPublisher emailEventPublisher;

    public AuthServiceImpl(UserRepository repository,
                           PasswordEncoder encoder,
                           JwtService jwtService,
                           EmailEventPublisher emailEventPublisher) {
        this.repository = repository;
        this.encoder = encoder;
        this.jwtService = jwtService;
        this.emailEventPublisher = emailEventPublisher;
    }

    @Override
    public void register(RegisterRequest request) throws UserAlreadyExistsException {

        if (repository.existsByEmail(request.getEmail())) {
            throw new UserAlreadyExistsException(request.getEmail());
        }

        User user = new User();
        user.setEmail(request.getEmail());
        user.setPassword(encoder.encode(request.getPassword()));
        user.setRole(request.getRole());

        User saved = repository.save(user);
        emailEventPublisher.publish(EmailNotificationEvent.builder()
                .eventType("USER_REGISTERED")
                .occurredAt(Instant.now())
                .userId(saved.getId())
                .userEmail(saved.getEmail())
                .role(saved.getRole().name())
                .build());
    }

    @Override
    public String login(LoginRequest request) {

        var user = repository.findByEmail(request.getEmail())
                .orElseThrow(() -> new UserNotFoundException("User not found"));

        if (!user.isEnabled()) {
            throw new InvalidCredentialsException("User is disabled");
        }

        if (!encoder.matches(request.getPassword(), user.getPassword())) {
            throw new InvalidCredentialsException("Invalid credentials");
        }

        return jwtService.generateToken(
                user.getId(),
                user.getEmail(),
                user.getRole().name()
        );

    }

    @Override
    public UserInternalResponse getUserInternal(Long userId) {
        User user = repository.findById(userId)
                .orElseThrow(() -> new UserNotFoundException("User not found"));
        return UserInternalResponse.builder()
                .userId(user.getId())
                .email(user.getEmail())
                .role(user.getRole().name())
                .build();
    }
}
