package com.modernbank.modernbank.controller;

import java.util.Map;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import com.modernbank.modernbank.exception.ApiErrorResponse;
@RestController
@RequestMapping("/api/v1/users")
public class UserController {

@Operation(
    summary = "Get current customer profile",
    description = "Returns the profile information of the " +
                  "currently authenticated customer using JWT authentication."
)
@ApiResponse(
    responseCode = "200",
    description = "Customer profile retrieved successfully"
)
@ApiResponse(
    responseCode = "401",
    description = "Authentication required or JWT is invalid",
    content = @Content(
        mediaType = "application/json",
        schema = @Schema(implementation = ApiErrorResponse.class)
    )
)

    @GetMapping("/me")
    public Map<String, String> getCurrentUser(
            @AuthenticationPrincipal Jwt jwt) {

        return Map.of(
                "userId", jwt.getSubject(),
                "email", jwt.getClaimAsString("email")
        );
    }
}