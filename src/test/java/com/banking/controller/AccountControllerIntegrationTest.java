package com.banking.controller;

import com.banking.dto.CreateAccountRequest;
import com.banking.dto.CreateUserRequest;
import com.banking.dto.DepositRequest;
import com.banking.dto.TransferRequest;
import com.banking.entity.AccountType;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.math.BigDecimal;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
class AccountControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    @DisplayName("End-to-End flow: Register users, open accounts, deposit, and transfer funds")
    void testFullBankingFlow() throws Exception {
        // 1. Create Customer 1
        CreateUserRequest user1Request = CreateUserRequest.builder()
                .firstName("Alice")
                .lastName("Smith")
                .email("alice.test@bank.com")
                .phoneNumber("+1234567890")
                .build();

        MvcResult user1Result = mockMvc.perform(post("/api/v1/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(user1Request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andReturn();

        Long user1Id = objectMapper.readTree(user1Result.getResponse().getContentAsString())
                .path("data").path("id").asLong();

        // 2. Create Customer 2
        CreateUserRequest user2Request = CreateUserRequest.builder()
                .firstName("Bob")
                .lastName("Taylor")
                .email("bob.test@bank.com")
                .phoneNumber("+1987654321")
                .build();

        MvcResult user2Result = mockMvc.perform(post("/api/v1/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(user2Request)))
                .andExpect(status().isCreated())
                .andReturn();

        Long user2Id = objectMapper.readTree(user2Result.getResponse().getContentAsString())
                .path("data").path("id").asLong();

        // 3. Open Account 1 with $1000 initial balance
        CreateAccountRequest account1Request = CreateAccountRequest.builder()
                .userId(user1Id)
                .accountType(AccountType.SAVINGS)
                .initialDeposit(new BigDecimal("1000.00"))
                .build();

        MvcResult acc1Result = mockMvc.perform(post("/api/v1/accounts")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(account1Request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.balance").value(1000.00))
                .andReturn();

        String account1Number = objectMapper.readTree(acc1Result.getResponse().getContentAsString())
                .path("data").path("accountNumber").asText();

        // 4. Open Account 2 with $100 initial balance
        CreateAccountRequest account2Request = CreateAccountRequest.builder()
                .userId(user2Id)
                .accountType(AccountType.CHECKING)
                .initialDeposit(new BigDecimal("100.00"))
                .build();

        MvcResult acc2Result = mockMvc.perform(post("/api/v1/accounts")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(account2Request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.balance").value(100.00))
                .andReturn();

        String account2Number = objectMapper.readTree(acc2Result.getResponse().getContentAsString())
                .path("data").path("accountNumber").asText();

        // 5. Deposit $250 into Account 1
        DepositRequest depositRequest = DepositRequest.builder()
                .accountNumber(account1Number)
                .amount(new BigDecimal("250.00"))
                .description("Bonus deposit")
                .build();

        mockMvc.perform(post("/api/v1/transactions/deposit")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(depositRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.balanceAfter").value(1250.00));

        // 6. Transfer $400 from Account 1 to Account 2
        TransferRequest transferRequest = TransferRequest.builder()
                .sourceAccountNumber(account1Number)
                .targetAccountNumber(account2Number)
                .amount(new BigDecimal("400.00"))
                .description("Peer to peer transfer")
                .build();

        mockMvc.perform(post("/api/v1/transactions/transfer")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(transferRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.balanceAfter").value(850.00));

        // 7. Verify Account 2 received funds: $100 + $400 = $500
        mockMvc.perform(get("/api/v1/accounts/" + account2Number))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.balance").value(500.00));

        // 8. Verify Account 1 transaction history
        mockMvc.perform(get("/api/v1/transactions/account/" + account1Number))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content").isArray());
    }

    @Test
    @DisplayName("Validation test: Reject registration when email format is invalid")
    void testInvalidEmailRegistration() throws Exception {
        CreateUserRequest invalidRequest = CreateUserRequest.builder()
                .firstName("Bad")
                .lastName("Email")
                .email("not-an-email")
                .phoneNumber("+1234567890")
                .build();

        mockMvc.perform(post("/api/v1/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidRequest)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.validationErrors.email").exists());
    }
}

