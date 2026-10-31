package com.modernbank.modernbank.controller;
import org.springframework.test.context.TestPropertySource;
import com.modernbank.modernbank.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import java.util.Map;
import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@TestPropertySource(properties = {
    "spring.datasource.url=jdbc:postgresql://localhost:5432/modernbank_test_db"
})
class BankAccountControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private JdbcTemplate jdbcTemplate;

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
    void authenticatedUserCanOpenSavingsAccount() throws Exception {
        String token = registerAndLogin();

        String requestBody = objectMapper.writeValueAsString(
                Map.of("accountType", "SAVINGS")
        );

        mockMvc.perform(post("/api/v1/accounts")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.accountNumber").isNotEmpty())
                .andExpect(jsonPath("$.accountType").value("SAVINGS"))
                .andExpect(jsonPath("$.currency").value("INR"))
                .andExpect(jsonPath("$.balance").value(0))
                .andExpect(jsonPath("$.status").value("ACTIVE"));
    }

    @Test
    void unauthenticatedUserCannotOpenAccount() throws Exception {
        String requestBody = objectMapper.writeValueAsString(
                Map.of("accountType", "SAVINGS")
        );

        mockMvc.perform(post("/api/v1/accounts")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void userCanOnlySeeTheirOwnAccounts() throws Exception {
        String firstUserToken = registerAndLogin();

        String requestBody = objectMapper.writeValueAsString(
                Map.of("accountType", "SAVINGS")
        );

        mockMvc.perform(post("/api/v1/accounts")
                        .header("Authorization", "Bearer " + firstUserToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isCreated());

        // Authenticate as a different user.
        String secondUserToken = registerAndLogin();

        mockMvc.perform(get("/api/v1/accounts")
                        .header("Authorization", "Bearer " + secondUserToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$.length()").value(0));
    }

    private String registerAndLogin() throws Exception {
        String email = "account-test-" + UUID.randomUUID() + "@example.com";
        String password = "StrongTestPassword@2026";

        String registrationBody = objectMapper.writeValueAsString(
                Map.of(
                        "firstName", "Test",
                        "lastName", "User",
                        "email", email,
                        "password", password
                )
        );

        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(registrationBody))
                .andExpect(status().isCreated());

        String loginBody = objectMapper.writeValueAsString(
                Map.of(
                        "email", email,
                        "password", password
                )
        );

        String response = mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(loginBody))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();

        JsonNode json = objectMapper.readTree(response);

        return json.get("accessToken").stringValue();
    }
}