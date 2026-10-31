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

import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.Map;
import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@TestPropertySource(properties = {
    "spring.datasource.url=jdbc:postgresql://localhost:5432/modernbank_test_db"
})
class BankStatementControllerTest {

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
                    "SAFETY ERROR: Tests cannot delete data from "
                            + databaseName
            );
        }
    }

    @BeforeEach
    void cleanDatabase() {
        verifyTestDatabase();

        jdbcTemplate.update("DELETE FROM bank_transactions");
        jdbcTemplate.update("DELETE FROM bank_transfers");
        jdbcTemplate.update("DELETE FROM bank_accounts");
        userRepository.deleteAll();
    }
     @Test
void statementWithNoTransactionsShouldPreserveBalance()
        throws Exception {

    String token = registerAndLogin();
    long accountId = createAccount(token);

    deposit(token, accountId, 1000);

    // Ensure we only modify the test database
    verifyTestDatabase();

    // Move the deposit before the statement period
    jdbcTemplate.update("""
        UPDATE bank_transactions
        SET created_at = ?
        WHERE bank_account_id = ?
          AND transaction_type = 'DEPOSIT'
        """,
        java.time.OffsetDateTime.parse("2026-10-10T10:00:00Z"),
        accountId
    );

    mockMvc.perform(
            get("/api/v1/accounts/{accountId}/statement", accountId)
                    .param("from", "2026-10-15")
                    .param("to", "2026-10-20")
                    .header("Authorization", "Bearer " + token)
    )
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.openingBalance").value(1000))
            .andExpect(jsonPath("$.totalCredits").value(0))
            .andExpect(jsonPath("$.totalDebits").value(0))
            .andExpect(jsonPath("$.closingBalance").value(1000))
            .andExpect(jsonPath("$.transactions.length()").value(0));
}
@Test
void statementShouldRespectDateBoundaries() throws Exception {

    String token = registerAndLogin();
    long accountId = createAccount(token);

    deposit(token, accountId, 1000);
    withdraw(token, accountId, 200);
    deposit(token, accountId, 500);

    verifyTestDatabase();

    // First deposit: before statement period
    jdbcTemplate.update("""
        UPDATE bank_transactions
        SET created_at = ?
        WHERE bank_account_id = ?
          AND transaction_type = 'DEPOSIT'
          AND amount = 1000
        """,
        java.time.OffsetDateTime.parse("2026-10-14T23:59:59Z"),
        accountId
    );

    // Withdrawal: exactly at statement start
    jdbcTemplate.update("""
        UPDATE bank_transactions
        SET created_at = ?
        WHERE bank_account_id = ?
          AND transaction_type = 'WITHDRAWAL'
        """,
        java.time.OffsetDateTime.parse("2026-10-15T00:00:00Z"),
        accountId
    );

    // Second deposit: last moment of statement period
    jdbcTemplate.update("""
        UPDATE bank_transactions
        SET created_at = ?
        WHERE bank_account_id = ?
          AND transaction_type = 'DEPOSIT'
          AND amount = 500
        """,
        java.time.OffsetDateTime.parse("2026-10-20T23:59:59Z"),
        accountId
    );

    mockMvc.perform(
            get("/api/v1/accounts/{accountId}/statement", accountId)
                    .param("from", "2026-10-15")
                    .param("to", "2026-10-20")
                    .header("Authorization", "Bearer " + token)
    )
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.openingBalance").value(1000))
            .andExpect(jsonPath("$.totalCredits").value(500))
            .andExpect(jsonPath("$.totalDebits").value(200))
            .andExpect(jsonPath("$.closingBalance").value(1300))
            .andExpect(jsonPath("$.transactions.length()").value(2))
            .andExpect(jsonPath("$.transactions[0].transactionType")
                    .value("WITHDRAWAL"))
            .andExpect(jsonPath("$.transactions[1].transactionType")
                    .value("DEPOSIT"));
}
@Test
void statementShouldExcludeTransactionAtEndBoundary()
        throws Exception {

    String token = registerAndLogin();
    long accountId = createAccount(token);

    deposit(token, accountId, 1000);
    withdraw(token, accountId, 200);

    verifyTestDatabase();

    // Deposit occurs inside the statement period
    jdbcTemplate.update("""
        UPDATE bank_transactions
        SET created_at = ?
        WHERE bank_account_id = ?
          AND transaction_type = 'DEPOSIT'
        """,
        java.time.OffsetDateTime.parse("2026-10-20T23:59:59Z"),
        accountId
    );

    // Withdrawal occurs exactly at the exclusive end boundary
    jdbcTemplate.update("""
        UPDATE bank_transactions
        SET created_at = ?
        WHERE bank_account_id = ?
          AND transaction_type = 'WITHDRAWAL'
        """,
        java.time.OffsetDateTime.parse("2026-10-21T00:00:00Z"),
        accountId
    );

    mockMvc.perform(
            get("/api/v1/accounts/{accountId}/statement", accountId)
                    .param("from", "2026-10-15")
                    .param("to", "2026-10-20")
                    .header("Authorization", "Bearer " + token)
    )
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.openingBalance").value(0))
            .andExpect(jsonPath("$.totalCredits").value(1000))
            .andExpect(jsonPath("$.totalDebits").value(0))
            .andExpect(jsonPath("$.closingBalance").value(1000))
            .andExpect(jsonPath("$.transactions.length()").value(1))
            .andExpect(jsonPath("$.transactions[0].transactionType")
                    .value("DEPOSIT"));
}
    @Test
    void ownerCanGenerateCorrectBankStatement() throws Exception {

        String token = registerAndLogin();
        long accountId = createAccount(token);

        deposit(token, accountId, 1000);
        withdraw(token, accountId, 200);

        String today = LocalDate.now(ZoneOffset.UTC).toString();

        mockMvc.perform(
                get("/api/v1/accounts/{accountId}/statement", accountId)
                        .param("from", today)
                        .param("to", today)
                        .header("Authorization", "Bearer " + token)
        )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accountId").value(accountId))
                .andExpect(jsonPath("$.openingBalance").value(0))
                .andExpect(jsonPath("$.totalCredits").value(1000))
                .andExpect(jsonPath("$.totalDebits").value(200))
                .andExpect(jsonPath("$.closingBalance").value(800))
                .andExpect(jsonPath("$.transactions.length()").value(2))
                .andExpect(jsonPath("$.transactions[0].transactionType")
                        .value("DEPOSIT"))
                .andExpect(jsonPath("$.transactions[1].transactionType")
                        .value("WITHDRAWAL"));
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

    private void deposit(String token, long accountId, int amount)
            throws Exception {

        String body = objectMapper.writeValueAsString(
                Map.of(
                        "amount", amount,
                        "description", "Test deposit"
                )
        );

        mockMvc.perform(
                post("/api/v1/accounts/{accountId}/deposits", accountId)
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body)
        ).andExpect(status().isCreated());
    }

    private void withdraw(String token, long accountId, int amount)
            throws Exception {

        String body = objectMapper.writeValueAsString(
                Map.of(
                        "amount", amount,
                        "description", "Test withdrawal"
                )
        );

        mockMvc.perform(
                post("/api/v1/accounts/{accountId}/withdrawals", accountId)
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body)
        ).andExpect(status().isCreated());
    }

    private String registerAndLogin() throws Exception {

        String email = "statement-test-"
                + UUID.randomUUID() + "@example.com";

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
    @Test
void anotherUserCannotViewBankStatement() throws Exception {

    // Customer A registers and creates an account
    String ownerToken = registerAndLogin();
    long accountId = createAccount(ownerToken);

    // Customer A deposits money
    deposit(ownerToken, accountId, 1000);

    // Customer B registers and logs in
    String anotherUserToken = registerAndLogin();

    String today = LocalDate.now(ZoneOffset.UTC).toString();

    // Customer B attempts to view Customer A's statement
    mockMvc.perform(
            get("/api/v1/accounts/{accountId}/statement", accountId)
                    .param("from", today)
                    .param("to", today)
                    .header("Authorization", "Bearer " + anotherUserToken)
    )
            .andExpect(status().isNotFound());
}
@Test
void invalidStatementDateRangesShouldReturnBadRequest()
        throws Exception {

    String token = registerAndLogin();
    long accountId = createAccount(token);

    // Case 1: Start date is after end date
    mockMvc.perform(
            get("/api/v1/accounts/{accountId}/statement", accountId)
                    .param("from", "2026-10-20")
                    .param("to", "2026-10-10")
                    .header("Authorization", "Bearer " + token)
    )
            .andExpect(status().isBadRequest());

    // Case 2: Date range exceeds three months
    mockMvc.perform(
            get("/api/v1/accounts/{accountId}/statement", accountId)
                    .param("from", "2026-01-01")
                    .param("to", "2026-06-01")
                    .header("Authorization", "Bearer " + token)
    )
            .andExpect(status().isBadRequest());
}
@Test
void statementShouldCalculateHistoricalOpeningBalance()
        throws Exception {

    String token = registerAndLogin();
    long accountId = createAccount(token);

    // Create two real transactions through the API
    deposit(token, accountId, 1000);
    withdraw(token, accountId, 200);

    // Safety check before modifying test data
    verifyTestDatabase();

    // Simulate transactions on historical dates
    jdbcTemplate.update("""
        UPDATE bank_transactions
        SET created_at = ?
        WHERE bank_account_id = ?
          AND transaction_type = 'DEPOSIT'
        """,
        java.time.OffsetDateTime.parse("2026-10-10T10:00:00Z"),
        accountId
    );

    jdbcTemplate.update("""
        UPDATE bank_transactions
        SET created_at = ?
        WHERE bank_account_id = ?
          AND transaction_type = 'WITHDRAWAL'
        """,
        java.time.OffsetDateTime.parse("2026-10-20T10:00:00Z"),
        accountId
    );

    // Request statement from October 15 to October 25
    mockMvc.perform(
            get("/api/v1/accounts/{accountId}/statement", accountId)
                    .param("from", "2026-10-15")
                    .param("to", "2026-10-25")
                    .header("Authorization", "Bearer " + token)
    )
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.openingBalance").value(1000))
            .andExpect(jsonPath("$.totalCredits").value(0))
            .andExpect(jsonPath("$.totalDebits").value(200))
            .andExpect(jsonPath("$.closingBalance").value(800))
            .andExpect(jsonPath("$.transactions.length()").value(1))
            .andExpect(jsonPath("$.transactions[0].transactionType")
                    .value("WITHDRAWAL"));
}
@Test
void statementShouldRejectMoreThan5000Transactions() throws Exception {

    String token = registerAndLogin();
    long accountId = createAccount(token);

    // Verify the test database before any direct database changes
    verifyTestDatabase();

    // Add 5001 transactions using one database operation
    jdbcTemplate.update("""
        INSERT INTO bank_transactions
            (reference_id, bank_account_id, transaction_type,
             amount, balance_after, description, created_at)
        SELECT
            gen_random_uuid(),
            ?,
            'DEPOSIT',
            1.00,
            1.00,
            'Statement limit test',
            TIMESTAMPTZ '2026-10-20 12:00:00+00'
        FROM generate_series(1, 5001)
        """, accountId);

    mockMvc.perform(
            get("/api/v1/accounts/{accountId}/statement", accountId)
                    .param("from", "2026-10-01")
                    .param("to", "2026-10-31")
                    .header("Authorization", "Bearer " + token)
    )
            .andExpect(status().isUnprocessableContent());
}
}