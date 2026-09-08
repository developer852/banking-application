package com.banking.dto;

import com.banking.entity.TransactionStatus;
import com.banking.entity.TransactionType;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TransactionResponse {
    private Long id;
    private String transactionReference;
    private TransactionType transactionType;
    private BigDecimal amount;
    private BigDecimal balanceAfter;
    private TransactionStatus status;
    private String sourceAccountNumber;
    private String targetAccountNumber;
    private String description;
    private LocalDateTime timestamp;
}

