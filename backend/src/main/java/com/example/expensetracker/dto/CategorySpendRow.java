package com.example.expensetracker.dto;

import java.math.BigDecimal;

public record CategorySpendRow(Long categoryId, String name, String color, String emoji, BigDecimal amount) {
}
