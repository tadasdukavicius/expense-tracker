package com.example.expensetracker.dto;

import java.math.BigDecimal;

public record CategorySpend(Long categoryId, String name, String color, String emoji,
                            BigDecimal amount, BigDecimal percent) {
}
