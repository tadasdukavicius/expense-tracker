package com.example.expensetracker.dto;

import java.math.BigDecimal;
import java.util.List;

public record CategoryStatsResponse(BigDecimal total, List<CategorySpend> categories) {
}
