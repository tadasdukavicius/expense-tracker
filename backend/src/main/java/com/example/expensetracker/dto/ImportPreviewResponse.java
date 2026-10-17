package com.example.expensetracker.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public record ImportPreviewResponse(
        Long batchId,
        String fileName,
        int totalRows,
        int newCount,
        int duplicateCount,
        List<Row> rows) {

    public record Row(
            int rowNumber,
            LocalDate bookingDate,
            BigDecimal amount,
            String currency,
            String description,
            String counterparty,
            String reference,
            boolean duplicate) {
    }
}