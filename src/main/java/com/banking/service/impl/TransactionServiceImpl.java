package com.banking.service.impl;

import com.banking.dto.DepositRequest;
import com.banking.dto.TransactionResponse;
import com.banking.dto.TransferRequest;
import com.banking.dto.WithdrawRequest;
import com.banking.entity.*;
import com.banking.exception.AccountInactiveException;
import com.banking.exception.InsufficientBalanceException;
import com.banking.exception.InvalidTransactionException;
import com.banking.exception.ResourceNotFoundException;
import com.banking.repository.AccountRepository;
import com.banking.repository.TransactionRepository;
import com.banking.service.TransactionService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class TransactionServiceImpl implements TransactionService {

    private final AccountRepository accountRepository;
    private final TransactionRepository transactionRepository;

    @Override
    @Transactional(isolation = Isolation.READ_COMMITTED, rollbackFor = Exception.class)
    public TransactionResponse deposit(DepositRequest request) {
        Account account = accountRepository.findByAccountNumberWithLock(request.getAccountNumber())
                .orElseThrow(() -> new ResourceNotFoundException("Account not found: " + request.getAccountNumber()));

        validateAccountActive(account);

        BigDecimal newBalance = account.getBalance().add(request.getAmount());
        account.setBalance(newBalance);
        accountRepository.save(account);

        Transaction transaction = Transaction.builder()
                .transactionReference(generateReference("DEP"))
                .transactionType(TransactionType.DEPOSIT)
                .amount(request.getAmount())
                .balanceAfter(newBalance)
                .status(TransactionStatus.SUCCESS)
                .targetAccount(account)
                .description(request.getDescription() != null ? request.getDescription() : "Cash Deposit")
                .build();

        Transaction savedTx = transactionRepository.save(transaction);
        return mapToResponse(savedTx);
    }

    @Override
    @Transactional(isolation = Isolation.READ_COMMITTED, rollbackFor = Exception.class)
    public TransactionResponse withdraw(WithdrawRequest request) {
        Account account = accountRepository.findByAccountNumberWithLock(request.getAccountNumber())
                .orElseThrow(() -> new ResourceNotFoundException("Account not found: " + request.getAccountNumber()));

        validateAccountActive(account);

        if (account.getBalance().compareTo(request.getAmount()) < 0) {
            throw new InsufficientBalanceException(
                    String.format("Insufficient balance in account %s. Current balance: %s, requested withdrawal: %s",
                            account.getAccountNumber(), account.getBalance(), request.getAmount()));
        }

        BigDecimal newBalance = account.getBalance().subtract(request.getAmount());
        account.setBalance(newBalance);
        accountRepository.save(account);

        Transaction transaction = Transaction.builder()
                .transactionReference(generateReference("WTH"))
                .transactionType(TransactionType.WITHDRAWAL)
                .amount(request.getAmount())
                .balanceAfter(newBalance)
                .status(TransactionStatus.SUCCESS)
                .sourceAccount(account)
                .description(request.getDescription() != null ? request.getDescription() : "Cash Withdrawal")
                .build();

        Transaction savedTx = transactionRepository.save(transaction);
        return mapToResponse(savedTx);
    }

    @Override
    @Transactional(isolation = Isolation.READ_COMMITTED, rollbackFor = Exception.class)
    public TransactionResponse transfer(TransferRequest request) {
        if (request.getSourceAccountNumber().trim().equals(request.getTargetAccountNumber().trim())) {
            throw new InvalidTransactionException("Source and destination accounts must be different");
        }

        // 1. Initial non-locking lookup to determine IDs for deadlock prevention
        Account sourceRef = accountRepository.findByAccountNumber(request.getSourceAccountNumber().trim())
                .orElseThrow(() -> new ResourceNotFoundException("Source account not found: " + request.getSourceAccountNumber()));

        Account targetRef = accountRepository.findByAccountNumber(request.getTargetAccountNumber().trim())
                .orElseThrow(() -> new ResourceNotFoundException("Destination account not found: " + request.getTargetAccountNumber()));

        // 2. Deadlock-free locking: Acquire pessimistic locks in deterministic order based on ID
        Account sourceAccount;
        Account targetAccount;

        if (sourceRef.getId() < targetRef.getId()) {
            sourceAccount = accountRepository.findByIdWithLock(sourceRef.getId())
                    .orElseThrow(() -> new ResourceNotFoundException("Source account not found"));
            targetAccount = accountRepository.findByIdWithLock(targetRef.getId())
                    .orElseThrow(() -> new ResourceNotFoundException("Destination account not found"));
        } else {
            targetAccount = accountRepository.findByIdWithLock(targetRef.getId())
                    .orElseThrow(() -> new ResourceNotFoundException("Destination account not found"));
            sourceAccount = accountRepository.findByIdWithLock(sourceRef.getId())
                    .orElseThrow(() -> new ResourceNotFoundException("Source account not found"));
        }

        // 3. Status and Balance validations
        validateAccountActive(sourceAccount);
        validateAccountActive(targetAccount);

        if (sourceAccount.getBalance().compareTo(request.getAmount()) < 0) {
            throw new InsufficientBalanceException(
                    String.format("Transfer failed: Insufficient balance in source account %s. Current balance: %s, transfer amount: %s",
                            sourceAccount.getAccountNumber(), sourceAccount.getBalance(), request.getAmount()));
        }

        // 4. Atomic balance mutations
        BigDecimal newSourceBalance = sourceAccount.getBalance().subtract(request.getAmount());
        BigDecimal newTargetBalance = targetAccount.getBalance().add(request.getAmount());

        sourceAccount.setBalance(newSourceBalance);
        targetAccount.setBalance(newTargetBalance);

        accountRepository.save(sourceAccount);
        accountRepository.save(targetAccount);

        // 5. Ledger entry
        String description = request.getDescription() != null && !request.getDescription().isBlank()
                ? request.getDescription()
                : String.format("Transfer from %s to %s", sourceAccount.getAccountNumber(), targetAccount.getAccountNumber());

        Transaction transaction = Transaction.builder()
                .transactionReference(generateReference("TRF"))
                .transactionType(TransactionType.TRANSFER)
                .amount(request.getAmount())
                .balanceAfter(newSourceBalance)
                .status(TransactionStatus.SUCCESS)
                .sourceAccount(sourceAccount)
                .targetAccount(targetAccount)
                .description(description)
                .build();

        Transaction savedTx = transactionRepository.save(transaction);
        return mapToResponse(savedTx);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<TransactionResponse> getAccountTransactions(String accountNumber, Pageable pageable) {
        if (!accountRepository.existsByAccountNumber(accountNumber)) {
            throw new ResourceNotFoundException("Account not found: " + accountNumber);
        }
        return transactionRepository.findAllByAccountNumber(accountNumber, pageable)
                .map(this::mapToResponse);
    }

    private void validateAccountActive(Account account) {
        if (account.getStatus() != AccountStatus.ACTIVE) {
            throw new AccountInactiveException(
                    String.format("Account %s is not active (status: %s)", account.getAccountNumber(), account.getStatus()));
        }
    }

    private String generateReference(String prefix) {
        return prefix + "-" + UUID.randomUUID().toString().replace("-", "").substring(0, 12).toUpperCase();
    }

    private TransactionResponse mapToResponse(Transaction tx) {
        return TransactionResponse.builder()
                .id(tx.getId())
                .transactionReference(tx.getTransactionReference())
                .transactionType(tx.getTransactionType())
                .amount(tx.getAmount())
                .balanceAfter(tx.getBalanceAfter())
                .status(tx.getStatus())
                .sourceAccountNumber(tx.getSourceAccount() != null ? tx.getSourceAccount().getAccountNumber() : null)
                .targetAccountNumber(tx.getTargetAccount() != null ? tx.getTargetAccount().getAccountNumber() : null)
                .description(tx.getDescription())
                .timestamp(tx.getTimestamp())
                .build();
    }
}

