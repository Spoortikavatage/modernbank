
package com.modernbank.modernbank.controller;

import com.modernbank.modernbank.dto.LoginRequest;
import com.modernbank.modernbank.dto.LoginResponse;
import com.modernbank.modernbank.dto.RegisterRequest;
import com.modernbank.modernbank.dto.UserResponse;
import com.modernbank.modernbank.service.AuthService;
import com.modernbank.modernbank.service.UserService;

import jakarta.validation.Valid;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import io.swagger.v3.oas.annotations.security.SecurityRequirements;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import com.modernbank.modernbank.exception.ApiErrorResponse;
@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {

    private final UserService userService;
    private final AuthService authService;

    public AuthController(
            UserService userService,
            AuthService authService) {
        this.userService = userService;
        this.authService = authService;
    }

@Operation(
    summary = "Register a new customer",
    description = "Creates a new customer account with a BCrypt-hashed password."
)
@ApiResponse(
    responseCode = "200",
    description = "Customer registered successfully"
)
@ApiResponse(
    responseCode = "400",
    description = "Invalid registration details",
    content = @Content(
        mediaType = "application/json",
        schema = @Schema(implementation = ApiErrorResponse.class)
    )
)
@ApiResponse(
    responseCode = "409",
    description = "Email address already registered",
    content = @Content(
        mediaType = "application/json",
        schema = @Schema(implementation = ApiErrorResponse.class)
    )
)
@SecurityRequirements()

    @PostMapping("/register")
    public ResponseEntity<UserResponse> register(
            @Valid @RequestBody RegisterRequest request) {

        UserResponse response = userService.register(request);

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(response);
    }

    @Operation(
    summary = "Customer login",
    description = "Authenticates a registered customer and returns a JWT access token."
)

@ApiResponse(
    responseCode = "200",
    description = "Login successful"
)
@ApiResponse(
    responseCode = "401",
    description = "Invalid email or password",
    content = @Content(
        mediaType = "application/json",
        schema = @Schema(implementation = ApiErrorResponse.class)
    )
)

@SecurityRequirements()
    @PostMapping("/login")
    public ResponseEntity<LoginResponse> login(
            @Valid @RequestBody LoginRequest request) {

        LoginResponse response = authService.login(request);

        return ResponseEntity.ok(response);
    }

}
