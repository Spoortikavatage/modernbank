package com.modernbank.modernbank.service;

import java.util.Locale;

import com.modernbank.modernbank.dto.LoginRequest;
import com.modernbank.modernbank.dto.LoginResponse;
import com.modernbank.modernbank.entity.User;
import com.modernbank.modernbank.exception.InvalidCredentialsException;
import com.modernbank.modernbank.repository.UserRepository;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;

    public AuthService(
            UserRepository userRepository,
            PasswordEncoder passwordEncoder,
            JwtService jwtService) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
    }

    public LoginResponse login(LoginRequest request) {

        String email = request.email()
                .trim()
                .toLowerCase(Locale.ROOT);

        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new InvalidCredentialsException(
                        "Invalid email or password"));

        if (!passwordEncoder.matches(
                request.password(), user.getPasswordHash())) {
            throw new InvalidCredentialsException(
                    "Invalid email or password");
        }

        return jwtService.generateToken(user);
    }
}