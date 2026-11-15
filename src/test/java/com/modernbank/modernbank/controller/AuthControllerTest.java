
package com.modernbank.modernbank.controller;
import org.springframework.test.context.TestPropertySource;
import tools.jackson.databind.ObjectMapper;

import com.modernbank.modernbank.dto.LoginRequest;
import com.modernbank.modernbank.dto.RegisterRequest;
import com.modernbank.modernbank.repository.UserRepository;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import org.springframework.jdbc.core.JdbcTemplate;


@SpringBootTest
@AutoConfigureMockMvc
@TestPropertySource(properties = {
    "spring.datasource.url=jdbc:postgresql://localhost:5432/modernbank_test_db"
})
class AuthControllerTest {

@Autowired
private JdbcTemplate jdbcTemplate;
    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UserRepository userRepository;

    private void verifyTestDatabase() {
    String databaseName = jdbcTemplate.queryForObject(
        "SELECT current_database()",
        String.class
    );

    if (!"modernbank_test_db".equals(databaseName)) {
        throw new IllegalStateException(
            "SAFETY ERROR: Tests cannot delete data from " + databaseName
        );
    }
}
    @BeforeEach
void cleanDatabase() {

    // Prevent accidental deletion from the development database
    verifyTestDatabase();

    // Delete child records before parent records
    jdbcTemplate.update("DELETE FROM bank_transactions");
    jdbcTemplate.update("DELETE FROM bank_transfers");
    jdbcTemplate.update("DELETE FROM bank_accounts");
    userRepository.deleteAll();
}

    @Test
    void shouldRegisterNewUser() throws Exception {
        RegisterRequest request = new RegisterRequest(
                "Test",
                "User",
                "newuser@example.com",
                "SecureTestPass@2026"
        );

        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated());
    }

    @Test
    void shouldRejectDuplicateEmail() throws Exception {
        RegisterRequest request = new RegisterRequest(
                "Test",
                "User",
                "duplicate@example.com",
                "SecureTestPass@2026"
        );

        String json = objectMapper.writeValueAsString(request);

        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isCreated());

        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isConflict());
    }

    @Test
    void shouldRejectInvalidEmail() throws Exception {
        RegisterRequest request = new RegisterRequest(
                "Test",
                "User",
                "not-an-email",
                "SecureTestPass@2026"
        );

        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }
    @Test
void shouldRejectPasswordExceeding72Utf8Bytes() throws Exception {

    String longPassword = "a".repeat(73);

    RegisterRequest request = new RegisterRequest(
            "Test",
            "User",
            "longpassword@example.com",
            longPassword
    );

    mockMvc.perform(post("/api/v1/auth/register")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isBadRequest());
}
@Test
void shouldRejectLoginPasswordExceeding72Utf8Bytes() throws Exception {

    String loginBody = objectMapper.writeValueAsString(
            java.util.Map.of(
                    "email", "nonexistent@example.com",
                    "password", "a".repeat(73)
            )
    );

    mockMvc.perform(post("/api/v1/auth/login")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(loginBody))
            .andExpect(status().isUnauthorized());
}
@Test
void shouldRejectRegistrationWithInvalidEmail() throws Exception {

    RegisterRequest request = new RegisterRequest(
            "Test",
            "User",
            "invalid-email",
            "StrongPassword@123"
    );

    mockMvc.perform(post("/api/v1/auth/register")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isBadRequest());
}
@Test
void shouldRejectRegistrationWithBlankEmail() throws Exception {

    RegisterRequest request = new RegisterRequest(
            "Test",
            "User",
            " ",
            "StrongPassword@123"
    );

    mockMvc.perform(post("/api/v1/auth/register")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isBadRequest());
}
@Test
void shouldRejectRegistrationWithBlankFirstName() throws Exception {
    RegisterRequest request = new RegisterRequest(
            " ", "User", "blankfirst@example.com", "StrongPassword@123");

    mockMvc.perform(post("/api/v1/auth/register")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isBadRequest());
}

@Test
void shouldRejectRegistrationWithBlankLastName() throws Exception {
    RegisterRequest request = new RegisterRequest(
            "Test", " ", "blanklast@example.com", "StrongPassword@123");

    mockMvc.perform(post("/api/v1/auth/register")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isBadRequest());
}

@Test
void shouldRejectRegistrationWithEmailExceeding255Characters()
        throws Exception {
    String longEmail = "a".repeat(246) + "@test.com";

    RegisterRequest request = new RegisterRequest(
            "Test", "User", longEmail, "StrongPassword@123");

    mockMvc.perform(post("/api/v1/auth/register")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isBadRequest());
}

@Test
void shouldRejectRegistrationWithPasswordShorterThan12Characters()
        throws Exception {
    RegisterRequest request = new RegisterRequest(
            "Test", "User", "shortpass@example.com", "Short@123");

    mockMvc.perform(post("/api/v1/auth/register")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isBadRequest());
}

@Test
void shouldRejectRegistrationWithBlankPassword() throws Exception {
    RegisterRequest request = new RegisterRequest(
            "Test", "User", "blankpass@example.com", " ");

    mockMvc.perform(post("/api/v1/auth/register")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isBadRequest());
}

@Test
void shouldRejectLoginWithInvalidEmail() throws Exception {
    LoginRequest request = new LoginRequest("invalid-email", "Password@123");

    mockMvc.perform(post("/api/v1/auth/login")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isBadRequest());
}

@Test
void shouldRejectLoginWithBlankEmail() throws Exception {
    LoginRequest request = new LoginRequest(" ", "Password@123");

    mockMvc.perform(post("/api/v1/auth/login")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isBadRequest());
}

@Test
void shouldRejectLoginWithBlankPassword() throws Exception {
    LoginRequest request = new LoginRequest("test@example.com", " ");

    mockMvc.perform(post("/api/v1/auth/login")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isBadRequest());
}
}
