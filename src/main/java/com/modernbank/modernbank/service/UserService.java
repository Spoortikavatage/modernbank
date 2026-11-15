
package com.modernbank.modernbank.service;

import com.modernbank.modernbank.dto.RegisterRequest;
import com.modernbank.modernbank.dto.UserResponse;
import com.modernbank.modernbank.entity.User;
import com.modernbank.modernbank.exception.EmailAlreadyExistsException;
import com.modernbank.modernbank.repository.UserRepository;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.http.HttpStatus;

@Service
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public UserService(
            UserRepository userRepository,
            PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Transactional
    public UserResponse register(RegisterRequest request) {

        String email = request.email().trim().toLowerCase(
                java.util.Locale.ROOT);

       if (userRepository.existsByEmail(email)) {
    throw new EmailAlreadyExistsException(
            "An account with this email already exists");
}
        int passwordBytes = request.password()
        .getBytes(StandardCharsets.UTF_8).length;

if (passwordBytes > 72) {
    throw new ResponseStatusException(
            HttpStatus.BAD_REQUEST,
            "Password must not exceed 72 UTF-8 bytes"
    );
}
        String passwordHash =
                passwordEncoder.encode(request.password());

        User user = new User(
                request.firstName().trim(),
                request.lastName().trim(),
                email,
                passwordHash
        );

        User savedUser = userRepository.save(user);

        return new UserResponse(
                savedUser.getId(),
                savedUser.getFirstName(),
                savedUser.getLastName(),
                savedUser.getEmail(),
                savedUser.getCreatedAt()
        );
    }
}
