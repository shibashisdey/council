package com.council.userauthenticationservice.service;

import com.council.userauthenticationservice.Repository.UserRepository;
import com.council.userauthenticationservice.exception.InvalidCredentialsException;
import com.council.userauthenticationservice.exception.UserAlreadyExistsException;
import com.council.userauthenticationservice.exception.UserNotFoundException;
import com.council.userauthenticationservice.model.LoginRequest;
import com.council.userauthenticationservice.model.RegisterRequest;
import com.council.userauthenticationservice.model.User;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
public class AuthServiceImpl implements AuthService {

    private final UserRepository repository;
    private final PasswordEncoder encoder;
    private final JwtService jwtService;

    public AuthServiceImpl(UserRepository repository,
                           PasswordEncoder encoder,
                           JwtService jwtService) {
        this.repository = repository;
        this.encoder = encoder;
        this.jwtService = jwtService;
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

        repository.save(user);
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
}
