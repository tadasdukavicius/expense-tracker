package com.example.expensetracker.dto;

import com.example.expensetracker.entity.MatchField;

public record CategoryRuleResponse(
        Long id,
        MatchField matchField,
        String pattern,
        CategoryResponse category,
        int priority) {
}
