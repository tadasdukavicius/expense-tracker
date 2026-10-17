package com.example.expensetracker.dto;

import com.example.expensetracker.entity.ImportBatchStatus;

public record ImportResultResponse(
        Long batchId,
        String fileName,
        ImportBatchStatus status,
        int importedCount,
        int skippedCount) {
}