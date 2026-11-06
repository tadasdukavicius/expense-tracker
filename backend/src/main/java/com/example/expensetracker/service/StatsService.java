package com.example.expensetracker.service;

import com.example.expensetracker.dto.CategorySpend;
import com.example.expensetracker.dto.CategorySpendRow;
import com.example.expensetracker.dto.CategoryStatsResponse;
import com.example.expensetracker.repository.TransactionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;

@Service
@RequiredArgsConstructor
public class StatsService {

    private static final LocalDate MIN_DATE = LocalDate.of(1900, 1, 1);
    private static final LocalDate MAX_DATE = LocalDate.of(9999, 12, 31);

    private final TransactionRepository repo;

    @Transactional(readOnly = true)
    public CategoryStatsResponse byCategory(LocalDate from, LocalDate to, Long accountId) {
        List<CategorySpendRow> rows = repo.spendByCategory(
                from == null ? MIN_DATE : from, to == null ? MAX_DATE : to, accountId);

        BigDecimal total = rows.stream().map(CategorySpendRow::amount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        List<CategorySpend> items = rows.stream()
                .map(r -> new CategorySpend(
                        r.categoryId(),
                        r.categoryId() == null ? "Uncategorized" : r.name(),
                        r.color() == null ? "#9e9e9e" : r.color(),
                        r.emoji(),
                        r.amount(),
                        percent(r.amount(), total)))
                .sorted(Comparator.comparing(CategorySpend::amount).reversed())
                .toList();

        return new CategoryStatsResponse(total, items);
    }

    private static BigDecimal percent(BigDecimal part, BigDecimal total) {
        if (total.signum() == 0) return BigDecimal.ZERO;
        return part.multiply(BigDecimal.valueOf(100)).divide(total, 1, RoundingMode.HALF_UP);
    }
}
