package com.banking.service;

import com.banking.dto.DepositRequest;
import com.banking.dto.TransactionResponse;
import com.banking.dto.TransferRequest;
import com.banking.dto.WithdrawRequest;
import com.banking.entity.*;
import com.banking.exception.AccountInactiveException;
import com.banking.exception.InsufficientBalanceException;
import com.banking.exception.InvalidTransactionException;
import com.banking.repository.AccountRepository;
import com.banking.repository.TransactionRepository;
import com.banking.service.impl.TransactionServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TransactionServiceTest {

    @Mock
    private AccountRepository accountRepository;

    @Mock
    private TransactionRepository transactionRepository;

    @InjectMocks
    private TransactionServiceImpl transactionService;

    private Account sourceAccount;
    private Account targetAccount;
    private User testUser;

    @BeforeEach
    void setUp() {
        testUser = User.builder()
                .id(1L)
                .firstName("John")
                .lastName("Doe")
                .email("john.doe@example.com")
                .phoneNumber("+1234567890")
                .build();

        sourceAccount = Account.builder()
                .id(101L)
                .accountNumber("1000000001")
                .accountType(AccountType.SAVINGS)
                .balance(new BigDecimal("1000.00"))
                .status(AccountStatus.ACTIVE)
                .user(testUser)
                .build();

        targetAccount = Account.builder()
                .id(102L)
                .accountNumber("1000000002")
                .accountType(AccountType.CHECKING)
                .balance(new BigDecimal("500.00"))
                .status(AccountStatus.ACTIVE)
                .user(testUser)
                .build();
    }

    @Test
    @DisplayName("Should successfully deposit funds and update balance")
    void testDeposit_Success() {
        DepositRequest request = DepositRequest.builder()
                .accountNumber("1000000001")
                .amount(new BigDecimal("250.00"))
                .description("Test deposit")
                .build();

        when(accountRepository.findByAccountNumberWithLock("1000000001")).thenReturn(Optional.of(sourceAccount));
        when(accountRepository.save(any(Account.class))).thenReturn(sourceAccount);
        when(transactionRepository.save(any(Transaction.class))).thenAnswer(invocation -> invocation.getArgument(0));

        TransactionResponse response = transactionService.deposit(request);

        assertNotNull(response);
        assertEquals(new BigDecimal("1250.00"), sourceAccount.getBalance());
        assertEquals(TransactionType.DEPOSIT, response.getTransactionType());
        verify(accountRepository).save(sourceAccount);
        verify(transactionRepository).save(any(Transaction.class));
    }

    @Test
    @DisplayName("Should successfully withdraw funds when balance is sufficient")
    void testWithdraw_Success() {
        WithdrawRequest request = WithdrawRequest.builder()
                .accountNumber("1000000001")
                .amount(new BigDecimal("300.00"))
                .description("Test withdrawal")
                .build();

        when(accountRepository.findByAccountNumberWithLock("1000000001")).thenReturn(Optional.of(sourceAccount));
        when(accountRepository.save(any(Account.class))).thenReturn(sourceAccount);
        when(transactionRepository.save(any(Transaction.class))).thenAnswer(invocation -> invocation.getArgument(0));

        TransactionResponse response = transactionService.withdraw(request);

        assertNotNull(response);
        assertEquals(new BigDecimal("700.00"), sourceAccount.getBalance());
        assertEquals(TransactionType.WITHDRAWAL, response.getTransactionType());
        verify(accountRepository).save(sourceAccount);
    }

    @Test
    @DisplayName("Should throw InsufficientBalanceException when withdrawal amount exceeds balance")
    void testWithdraw_InsufficientBalance() {
        WithdrawRequest request = WithdrawRequest.builder()
                .accountNumber("1000000001")
                .amount(new BigDecimal("1500.00"))
                .build();

        when(accountRepository.findByAccountNumberWithLock("1000000001")).thenReturn(Optional.of(sourceAccount));

        assertThrows(InsufficientBalanceException.class, () -> transactionService.withdraw(request));
        verify(accountRepository, never()).save(any());
        verify(transactionRepository, never()).save(any());
    }

    @Test
    @DisplayName("Should throw AccountInactiveException when account is not active")
    void testWithdraw_InactiveAccount() {
        sourceAccount.setStatus(AccountStatus.SUSPENDED);
        WithdrawRequest request = WithdrawRequest.builder()
                .accountNumber("1000000001")
                .amount(new BigDecimal("50.00"))
                .build();

        when(accountRepository.findByAccountNumberWithLock("1000000001")).thenReturn(Optional.of(sourceAccount));

        assertThrows(AccountInactiveException.class, () -> transactionService.withdraw(request));
    }

    @Test
    @DisplayName("Should successfully transfer funds between accounts with deadlock-free ordering")
    void testTransfer_Success() {
        TransferRequest request = TransferRequest.builder()
                .sourceAccountNumber("1000000001")
                .targetAccountNumber("1000000002")
                .amount(new BigDecimal("400.00"))
                .description("Transfer for bills")
                .build();

        when(accountRepository.findByAccountNumber("1000000001")).thenReturn(Optional.of(sourceAccount));
        when(accountRepository.findByAccountNumber("1000000002")).thenReturn(Optional.of(targetAccount));

        // Source ID is 101, Target ID is 102 -> ordered acquisition
        when(accountRepository.findByIdWithLock(101L)).thenReturn(Optional.of(sourceAccount));
        when(accountRepository.findByIdWithLock(102L)).thenReturn(Optional.of(targetAccount));
        when(transactionRepository.save(any(Transaction.class))).thenAnswer(invocation -> invocation.getArgument(0));

        TransactionResponse response = transactionService.transfer(request);

        assertNotNull(response);
        assertEquals(new BigDecimal("600.00"), sourceAccount.getBalance());
        assertEquals(new BigDecimal("900.00"), targetAccount.getBalance());
        assertEquals(TransactionType.TRANSFER, response.getTransactionType());
        verify(accountRepository).save(sourceAccount);
        verify(accountRepository).save(targetAccount);
    }

    @Test
    @DisplayName("Should reject transfer when source and destination are the same account")
    void testTransfer_SameAccount_Fails() {
        TransferRequest request = TransferRequest.builder()
                .sourceAccountNumber("1000000001")
                .targetAccountNumber("1000000001")
                .amount(new BigDecimal("100.00"))
                .build();

        assertThrows(InvalidTransactionException.class, () -> transactionService.transfer(request));
    }
}

