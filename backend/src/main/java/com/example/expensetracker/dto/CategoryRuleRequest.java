package com.example.expensetracker.dto;

import com.example.expensetracker.entity.MatchField;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record CategoryRuleRequest(
        @NotNull MatchField matchField,
        @NotBlank @Size(max = 255) String pattern,
        @NotNull Long categoryId,
        @Min(0) Integer priority) {
}
