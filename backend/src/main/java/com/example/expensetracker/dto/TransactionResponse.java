package com.example.expensetracker.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

public record TransactionResponse(
        Long id,
        LocalDate bookingDate,
        BigDecimal amount,
        String currency,
        String description,
        String counterparty,
        CategoryResponse category) {
}