
package com.modernbank.modernbank.controller;

import com.modernbank.modernbank.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.TestPropertySource;
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
class BankTransactionControllerTest {

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
void ownerCanViewTransactionHistory() throws Exception {
    String token = registerAndLogin();
    long accountId = createAccount(token);

    deposit(token, accountId, 1000);
    withdraw(token, accountId, 200);

    mockMvc.perform(get("/api/v1/accounts/{accountId}/transactions", accountId)
                    .param("page", "0")
                    .param("size", "10")
                    .header("Authorization", "Bearer " + token))
            .andExpect(status().isOk())

            // Verify pagination metadata
            .andExpect(jsonPath("$.content.length()").value(2))
            .andExpect(jsonPath("$.totalElements").value(2))
            .andExpect(jsonPath("$.totalPages").value(1))
            .andExpect(jsonPath("$.number").value(0))
            .andExpect(jsonPath("$.size").value(10))

            // Verify newest transaction: withdrawal
            .andExpect(jsonPath("$.content[0].transactionType")
                    .value("WITHDRAWAL"))
            .andExpect(jsonPath("$.content[0].amount").value(200))
            .andExpect(jsonPath("$.content[0].balanceAfter").value(800))

            // Verify older transaction: deposit
            .andExpect(jsonPath("$.content[1].transactionType")
                    .value("DEPOSIT"))
            .andExpect(jsonPath("$.content[1].amount").value(1000))
            .andExpect(jsonPath("$.content[1].balanceAfter").value(1000));
}

    @Test
    void anotherUserCannotViewTransactionHistory() throws Exception {
        String ownerToken = registerAndLogin();
        long accountId = createAccount(ownerToken);

        deposit(ownerToken, accountId, 1000);

        String anotherUserToken = registerAndLogin();

        mockMvc.perform(get("/api/v1/accounts/{accountId}/transactions", accountId)
                        .header("Authorization", "Bearer " + anotherUserToken))
                .andExpect(status().isNotFound());
    }

   @Test
void transactionsAreReturnedNewestFirst() throws Exception {
    String token = registerAndLogin();
    long accountId = createAccount(token);

    // Create three transactions in sequence
    deposit(token, accountId, 1000);
    withdraw(token, accountId, 200);
    deposit(token, accountId, 500);

    mockMvc.perform(
            get("/api/v1/accounts/{accountId}/transactions", accountId)
                    .param("page", "0")
                    .param("size", "10")
                    .header("Authorization", "Bearer " + token)
    )
            .andExpect(status().isOk())

            // Verify pagination metadata
            .andExpect(jsonPath("$.content.length()").value(3))
            .andExpect(jsonPath("$.totalElements").value(3))
            .andExpect(jsonPath("$.totalPages").value(1))

            // Newest transaction: deposit ₹500
            .andExpect(jsonPath("$.content[0].transactionType")
                    .value("DEPOSIT"))
            .andExpect(jsonPath("$.content[0].amount").value(500))

            // Second transaction: withdrawal ₹200
            .andExpect(jsonPath("$.content[1].transactionType")
                    .value("WITHDRAWAL"))
            .andExpect(jsonPath("$.content[1].amount").value(200))

            // Oldest transaction: deposit ₹1000
            .andExpect(jsonPath("$.content[2].transactionType")
                    .value("DEPOSIT"))
            .andExpect(jsonPath("$.content[2].amount").value(1000));
}

    @Test
    void unauthenticatedUserCannotViewHistory() throws Exception {
        mockMvc.perform(get("/api/v1/accounts/1/transactions"))
                .andExpect(status().isUnauthorized());
    }
    @Test
void transactionHistoryShouldSupportPagination() throws Exception {

    // Step 1: Register and log in
    String token = registerAndLogin();

    // Step 2: Create a bank account
    long accountId = createAccount(token);

    // Step 3: Create three transactions
    deposit(token, accountId, 1000);
    withdraw(token, accountId, 200);
    deposit(token, accountId, 500);

    // Step 4: Request the first page (2 transactions)
    mockMvc.perform(
            get("/api/v1/accounts/{accountId}/transactions", accountId)
                    .param("page", "0")
                    .param("size", "2")
                    .header("Authorization", "Bearer " + token)
    )
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.content.length()").value(2))
            .andExpect(jsonPath("$.totalElements").value(3))
            .andExpect(jsonPath("$.totalPages").value(2))
            .andExpect(jsonPath("$.number").value(0))
            .andExpect(jsonPath("$.first").value(true))
            .andExpect(jsonPath("$.last").value(false))
            .andExpect(jsonPath("$.content[0].transactionType")
                    .value("DEPOSIT"))
            .andExpect(jsonPath("$.content[0].amount").value(500))
            .andExpect(jsonPath("$.content[1].transactionType")
                    .value("WITHDRAWAL"));

    // Step 5: Request the second page (1 transaction)
    mockMvc.perform(
            get("/api/v1/accounts/{accountId}/transactions", accountId)
                    .param("page", "1")
                    .param("size", "2")
                    .header("Authorization", "Bearer " + token)
    )
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.content.length()").value(1))
            .andExpect(jsonPath("$.totalElements").value(3))
            .andExpect(jsonPath("$.totalPages").value(2))
            .andExpect(jsonPath("$.number").value(1))
            .andExpect(jsonPath("$.first").value(false))
            .andExpect(jsonPath("$.last").value(true))
            .andExpect(jsonPath("$.content[0].transactionType")
                    .value("DEPOSIT"))
            .andExpect(jsonPath("$.content[0].amount").value(1000));
}

@Test
void invalidPaginationParametersShouldReturnBadRequest()
        throws Exception {

    String token = registerAndLogin();
    long accountId = createAccount(token);

    // Negative page number
    mockMvc.perform(
            get("/api/v1/accounts/{accountId}/transactions", accountId)
                    .param("page", "-1")
                    .param("size", "10")
                    .header("Authorization", "Bearer " + token)
    )
            .andExpect(status().isBadRequest());

    // Page size cannot be zero
    mockMvc.perform(
            get("/api/v1/accounts/{accountId}/transactions", accountId)
                    .param("page", "0")
                    .param("size", "0")
                    .header("Authorization", "Bearer " + token)
    )
            .andExpect(status().isBadRequest());

    // Maximum allowed page size is 100
    mockMvc.perform(
            get("/api/v1/accounts/{accountId}/transactions", accountId)
                    .param("page", "0")
                    .param("size", "101")
                    .header("Authorization", "Bearer " + token)
    )
            .andExpect(status().isBadRequest());
}

    private long createAccount(String token) throws Exception {
        String body = objectMapper.writeValueAsString(
                Map.of("accountType", "SAVINGS")
        );

        String response = mockMvc.perform(post("/api/v1/accounts")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString();

        return objectMapper.readTree(response).get("id").asLong();
    }

    private void deposit(String token, long accountId, int amount) throws Exception {
        String body = objectMapper.writeValueAsString(
                Map.of(
                        "amount", amount,
                        "description", "Test deposit"
                )
        );

        mockMvc.perform(post("/api/v1/accounts/{accountId}/deposits", accountId)
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isCreated());
    }

    private void withdraw(String token, long accountId, int amount) throws Exception {
        String body = objectMapper.writeValueAsString(
                Map.of(
                        "amount", amount,
                        "description", "Test withdrawal"
                )
        );

        mockMvc.perform(post("/api/v1/accounts/{accountId}/withdrawals", accountId)
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isCreated());
    }

    private String registerAndLogin() throws Exception {
        String email = "transaction-test-" + UUID.randomUUID() + "@example.com";
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
