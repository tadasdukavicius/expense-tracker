package com.example.expensetracker.mapper;

import com.example.expensetracker.dto.CategoryResponse;
import com.example.expensetracker.dto.TransactionResponse;
import com.example.expensetracker.entity.Category;
import com.example.expensetracker.entity.Transaction;

public final class Mapper {

    private Mapper() {
    }

    public static CategoryResponse toResponse(Category c) {
        return new CategoryResponse(c.getId(), c.getName(), c.getColor());
    }

    public static TransactionResponse toResponse(Transaction t) {
        return new TransactionResponse(
                t.getId(), t.getBookingDate(), t.getAmount(), t.getCurrency(),
                t.getDescription(), t.getCounterparty(),
                t.getCategory() == null ? null : toResponse(t.getCategory()));
    }
}