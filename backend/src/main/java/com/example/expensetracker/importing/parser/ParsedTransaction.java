package com.example.expensetracker.importing.parser;

import java.math.BigDecimal;
import java.time.LocalDate;

public record ParsedTransaction(
        LocalDate bookingDate,
        BigDecimal amount,
        String currency,
        String description,
        String counterparty,
        String reference) {
}