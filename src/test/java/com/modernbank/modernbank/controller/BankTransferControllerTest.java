
package com.modernbank.modernbank.controller;

import com.modernbank.modernbank.repository.UserRepository;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import tools.jackson.databind.ObjectMapper;
//import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import tools.jackson.databind.JsonNode;

import java.util.Map;
import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.Callable;
//import java.util.concurrent.ExecutionException;

//import org.springframework.test.web.servlet.MvcResult;
@SpringBootTest
@AutoConfigureMockMvc
@TestPropertySource(properties = {
    "spring.datasource.url=jdbc:postgresql://localhost:5432/modernbank_test_db"
})
class BankTransferControllerTest {

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
void successfulTransferMovesMoneyBetweenAccounts() throws Exception {

    // 1. Register and log in as a test customer
    String token = registerAndLogin();

    // 2. Create two accounts belonging to that customer
    long senderId = createAccount(token);
    long receiverId = createAccount(token);

    // 3. Give the sender an initial balance of 1000
    deposit(token, senderId, 1000);

    // 4. Prepare the transfer request
    String transferBody = objectMapper.writeValueAsString(
        Map.of(
            "fromAccountId", senderId,
            "toAccountId", receiverId,
            "amount", 100,
            "description", "Integration test transfer",
            "idempotencyKey", UUID.randomUUID().toString()
        )
    );

    // 5. Call the actual transfer API
    mockMvc.perform(post("/api/v1/transfers")
            .header("Authorization", "Bearer " + token)
            .contentType(MediaType.APPLICATION_JSON)
            .content(transferBody))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.fromAccountId").value(senderId))
        .andExpect(jsonPath("$.toAccountId").value(receiverId))
        .andExpect(jsonPath("$.amount").value(100))
        .andExpect(jsonPath("$.referenceId").exists());

    // 6. Verify actual balances in PostgreSQL
    java.math.BigDecimal senderBalance = jdbcTemplate.queryForObject(
        "SELECT balance FROM bank_accounts WHERE id = ?",
        java.math.BigDecimal.class,
        senderId
    );

    java.math.BigDecimal receiverBalance = jdbcTemplate.queryForObject(
        "SELECT balance FROM bank_accounts WHERE id = ?",
        java.math.BigDecimal.class,
        receiverId
    );

    org.junit.jupiter.api.Assertions.assertEquals(
        0, senderBalance.compareTo(new java.math.BigDecimal("900.00"))
    );

    org.junit.jupiter.api.Assertions.assertEquals(
        0, receiverBalance.compareTo(new java.math.BigDecimal("100.00"))
    );
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


@Test
void retryingSameTransferDoesNotMoveMoneyTwice() throws Exception {

    // 1. Register a customer and create two accounts
    String token = registerAndLogin();

    long senderId = createAccount(token);
    long receiverId = createAccount(token);

    // 2. Deposit 1000 into sender account
    deposit(token, senderId, 1000);

    // 3. Create one request with one idempotency key
    String transferBody = objectMapper.writeValueAsString(
        Map.of(
            "fromAccountId", senderId,
            "toAccountId", receiverId,
            "amount", 100,
            "description", "Idempotency test",
            "idempotencyKey", UUID.randomUUID().toString()
        )
    );

    // 4. First transfer
    String firstResponse = mockMvc.perform(
            post("/api/v1/transfers")
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content(transferBody))
        .andExpect(status().isCreated())
        .andReturn()
        .getResponse()
        .getContentAsString();

    // 5. Retry the exact same request
    String secondResponse = mockMvc.perform(
            post("/api/v1/transfers")
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content(transferBody))
        .andExpect(status().isCreated())
        .andReturn()
        .getResponse()
        .getContentAsString();

    // 6. Compare the transfer reference IDs
    String firstReference = objectMapper.readTree(firstResponse)
        .get("referenceId").stringValue();

    String secondReference = objectMapper.readTree(secondResponse)
        .get("referenceId").stringValue();

    org.junit.jupiter.api.Assertions.assertEquals(
        firstReference, secondReference
    );

    // 7. Verify sender was debited only once
    java.math.BigDecimal senderBalance = jdbcTemplate.queryForObject(
        "SELECT balance FROM bank_accounts WHERE id = ?",
        java.math.BigDecimal.class,
        senderId
    );

    // 8. Verify receiver was credited only once
    java.math.BigDecimal receiverBalance = jdbcTemplate.queryForObject(
        "SELECT balance FROM bank_accounts WHERE id = ?",
        java.math.BigDecimal.class,
        receiverId
    );

    org.junit.jupiter.api.Assertions.assertEquals(
        0, senderBalance.compareTo(new java.math.BigDecimal("900.00"))
    );

    org.junit.jupiter.api.Assertions.assertEquals(
        0, receiverBalance.compareTo(new java.math.BigDecimal("100.00"))
    );

    // 9. Verify only one transfer record exists
    Integer transferCount = jdbcTemplate.queryForObject(
        "SELECT COUNT(*) FROM bank_transfers WHERE from_account_id = ?",
        Integer.class,
        senderId
    );

    org.junit.jupiter.api.Assertions.assertEquals(1, transferCount);
}

@Test
void transferFailsWhenSenderHasInsufficientBalance() throws Exception {

    // 1. Register and log in
    String token = registerAndLogin();

    // 2. Create sender and receiver accounts
    long senderId = createAccount(token);
    long receiverId = createAccount(token);

    // 3. Deposit only 500 into sender
    deposit(token, senderId, 500);

    // 4. Attempt to transfer 1000
    String transferBody = objectMapper.writeValueAsString(
        Map.of(
            "fromAccountId", senderId,
            "toAccountId", receiverId,
            "amount", 1000,
            "description", "Insufficient balance test",
            "idempotencyKey", UUID.randomUUID().toString()
        )
    );

    // 5. Verify API rejects the transfer
    mockMvc.perform(post("/api/v1/transfers")
            .header("Authorization", "Bearer " + token)
            .contentType(MediaType.APPLICATION_JSON)
            .content(transferBody))
        .andExpect(status().isConflict());

    // 6. Verify sender balance remains 500
    java.math.BigDecimal senderBalance = jdbcTemplate.queryForObject(
        "SELECT balance FROM bank_accounts WHERE id = ?",
        java.math.BigDecimal.class,
        senderId
    );

    // 7. Verify receiver balance remains zero
    java.math.BigDecimal receiverBalance = jdbcTemplate.queryForObject(
        "SELECT balance FROM bank_accounts WHERE id = ?",
        java.math.BigDecimal.class,
        receiverId
    );

    org.junit.jupiter.api.Assertions.assertEquals(
        0, senderBalance.compareTo(new java.math.BigDecimal("500.00"))
    );

    org.junit.jupiter.api.Assertions.assertEquals(
        0, receiverBalance.compareTo(new java.math.BigDecimal("0.00"))
    );

    // 8. Verify no transfer record was created
    Integer transferCount = jdbcTemplate.queryForObject(
        "SELECT COUNT(*) FROM bank_transfers WHERE from_account_id = ?",
        Integer.class,
        senderId
    );

    org.junit.jupiter.api.Assertions.assertEquals(0, transferCount);
}


@Test
void anotherUserCannotTransferFromSomeoneElsesAccount() throws Exception {

    // 1. Register and log in as Customer A
    String customerAToken = registerAndLogin();

    // 2. Create two accounts owned by Customer A
    long senderId = createAccount(customerAToken);
    long receiverId = createAccount(customerAToken);

    // 3. Deposit 1000 into Customer A's sender account
    deposit(customerAToken, senderId, 1000);

    // 4. Register and log in as Customer B
    String customerBToken = registerAndLogin();

    // 5. Customer B attempts to transfer Customer A's money
    String transferBody = objectMapper.writeValueAsString(
        Map.of(
            "fromAccountId", senderId,
            "toAccountId", receiverId,
            "amount", 100,
            "description", "Unauthorized transfer test",
            "idempotencyKey", UUID.randomUUID().toString()
        )
    );

    mockMvc.perform(post("/api/v1/transfers")
            .header("Authorization", "Bearer " + customerBToken)
            .contentType(MediaType.APPLICATION_JSON)
            .content(transferBody))
        .andExpect(status().isNotFound());

    // 6. Verify sender balance remains unchanged
    java.math.BigDecimal senderBalance = jdbcTemplate.queryForObject(
        "SELECT balance FROM bank_accounts WHERE id = ?",
        java.math.BigDecimal.class,
        senderId
    );

    // 7. Verify receiver balance remains unchanged
    java.math.BigDecimal receiverBalance = jdbcTemplate.queryForObject(
        "SELECT balance FROM bank_accounts WHERE id = ?",
        java.math.BigDecimal.class,
        receiverId
    );

    org.junit.jupiter.api.Assertions.assertEquals(
        0, senderBalance.compareTo(new java.math.BigDecimal("1000.00"))
    );

    org.junit.jupiter.api.Assertions.assertEquals(
        0, receiverBalance.compareTo(new java.math.BigDecimal("0.00"))
    );

    // 8. Verify no transfer record was created
    Integer transferCount = jdbcTemplate.queryForObject(
        "SELECT COUNT(*) FROM bank_transfers WHERE from_account_id = ?",
        Integer.class,
        senderId
    );

    org.junit.jupiter.api.Assertions.assertEquals(0, transferCount);
}


@Test
void transferToSameAccountShouldFail() throws Exception {

    // 1. Register and log in
    String token = registerAndLogin();

    // 2. Create one account
    long accountId = createAccount(token);

    // 3. Deposit 1000
    deposit(token, accountId, 1000);

    // 4. Try transferring money to the same account
    String transferBody = objectMapper.writeValueAsString(
        Map.of(
            "fromAccountId", accountId,
            "toAccountId", accountId,
            "amount", 100,
            "description", "Same account transfer test",
            "idempotencyKey", UUID.randomUUID().toString()
        )
    );

    // 5. Expect HTTP 400 Bad Request
    mockMvc.perform(post("/api/v1/transfers")
            .header("Authorization", "Bearer " + token)
            .contentType(MediaType.APPLICATION_JSON)
            .content(transferBody))
        .andExpect(status().isBadRequest());

    // 6. Check account balance remains unchanged
    java.math.BigDecimal balance = jdbcTemplate.queryForObject(
        "SELECT balance FROM bank_accounts WHERE id = ?",
        java.math.BigDecimal.class,
        accountId
    );

    org.junit.jupiter.api.Assertions.assertEquals(
        0, balance.compareTo(new java.math.BigDecimal("1000.00"))
    );

    // 7. Verify no transfer record was created
    Integer transferCount = jdbcTemplate.queryForObject(
        "SELECT COUNT(*) FROM bank_transfers WHERE from_account_id = ?",
        Integer.class,
        accountId
    );

    org.junit.jupiter.api.Assertions.assertEquals(0, transferCount);
}

@Test
void concurrentTransfersCannotOverdrawAccount() throws Exception {

    // 1. Create a customer and three accounts
    String token = registerAndLogin();

    long senderId = createAccount(token);
    long receiver1Id = createAccount(token);
    long receiver2Id = createAccount(token);

    // 2. Deposit 1000 into the sender account
    deposit(token, senderId, 1000);

    // 3. Prepare two different transfer requests
    String body1 = objectMapper.writeValueAsString(
        Map.of(
            "fromAccountId", senderId,
            "toAccountId", receiver1Id,
            "amount", 800,
            "description", "Concurrent transfer 1",
            "idempotencyKey", UUID.randomUUID().toString()
        )
    );

    String body2 = objectMapper.writeValueAsString(
        Map.of(
            "fromAccountId", senderId,
            "toAccountId", receiver2Id,
            "amount", 800,
            "description", "Concurrent transfer 2",
            "idempotencyKey", UUID.randomUUID().toString()
        )
    );

    // 4. Prepare two threads
    ExecutorService executor = Executors.newFixedThreadPool(2);

    CountDownLatch ready = new CountDownLatch(2);
    CountDownLatch start = new CountDownLatch(1);

    try {

        Callable<Integer> task1 = () -> {
            ready.countDown();
            start.await();

            return mockMvc.perform(post("/api/v1/transfers")
                    .header("Authorization", "Bearer " + token)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(body1))
                .andReturn()
                .getResponse()
                .getStatus();
        };

        Callable<Integer> task2 = () -> {
            ready.countDown();
            start.await();

            return mockMvc.perform(post("/api/v1/transfers")
                    .header("Authorization", "Bearer " + token)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(body2))
                .andReturn()
                .getResponse()
                .getStatus();
        };

        // 5. Submit both tasks
        Future<Integer> future1 = executor.submit(task1);
        Future<Integer> future2 = executor.submit(task2);

        // 6. Wait until both threads are ready
        org.junit.jupiter.api.Assertions.assertTrue(
            ready.await(10, TimeUnit.SECONDS),
            "Both threads should become ready"
        );

        // 7. Release both threads together
        start.countDown();

        int status1 = future1.get(20, TimeUnit.SECONDS);
        int status2 = future2.get(20, TimeUnit.SECONDS);

        // 8. Exactly one transfer should succeed
        java.util.List<Integer> statuses =
            java.util.List.of(status1, status2);

        org.junit.jupiter.api.Assertions.assertEquals(
            1,
            statuses.stream().filter(s -> s == 201).count()
        );

        org.junit.jupiter.api.Assertions.assertEquals(
            1,
            statuses.stream().filter(s -> s == 409).count()
        );

        // 9. Verify sender's final balance
        java.math.BigDecimal senderBalance =
            jdbcTemplate.queryForObject(
                "SELECT balance FROM bank_accounts WHERE id = ?",
                java.math.BigDecimal.class,
                senderId
            );

        org.junit.jupiter.api.Assertions.assertEquals(
            0,
            senderBalance.compareTo(
                new java.math.BigDecimal("200.00")
            )
        );

        // 10. Verify exactly one transfer was recorded
        Integer transferCount = jdbcTemplate.queryForObject(
            "SELECT COUNT(*) FROM bank_transfers WHERE from_account_id = ?",
            Integer.class,
            senderId
        );

        org.junit.jupiter.api.Assertions.assertEquals(
            1, transferCount
        );

        // 11. Verify only one receiver got the money
        java.math.BigDecimal receiver1Balance =
            jdbcTemplate.queryForObject(
                "SELECT balance FROM bank_accounts WHERE id = ?",
                java.math.BigDecimal.class,
                receiver1Id
            );

        java.math.BigDecimal receiver2Balance =
            jdbcTemplate.queryForObject(
                "SELECT balance FROM bank_accounts WHERE id = ?",
                java.math.BigDecimal.class,
                receiver2Id
            );

        java.math.BigDecimal totalReceived =
            receiver1Balance.add(receiver2Balance);

        org.junit.jupiter.api.Assertions.assertEquals(
            0,
            totalReceived.compareTo(
                new java.math.BigDecimal("800.00")
            )
        );

    } finally {
        // Release waiting threads even if the test fails
        start.countDown();
        executor.shutdownNow();
    }
}


}
