
package com.modernbank.modernbank.service;

import com.modernbank.modernbank.dto.RegisterRequest;
import com.modernbank.modernbank.dto.UserResponse;
import com.modernbank.modernbank.entity.User;
import com.modernbank.modernbank.exception.EmailAlreadyExistsException;
import com.modernbank.modernbank.repository.UserRepository;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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
