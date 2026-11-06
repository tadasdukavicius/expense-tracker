package com.example.expensetracker.controller;

import com.example.expensetracker.dto.CategoryChangeRequest;
import com.example.expensetracker.dto.TransactionRequest;
import com.example.expensetracker.dto.TransactionResponse;
import com.example.expensetracker.service.TransactionService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import com.example.expensetracker.dto.RecategorizeResponse;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;

@RestController
@RequestMapping("/api/transactions")
@RequiredArgsConstructor
public class TransactionController {

    private final TransactionService service;

    @GetMapping
    public Page<TransactionResponse> search(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            @RequestParam(required = false) Long categoryId,
            @RequestParam(required = false, name = "q") String text,
            @RequestParam(required = false) Long accountId,
            @PageableDefault(size = 20, sort = "bookingDate", direction = org.springframework.data.domain.Sort.Direction.DESC)
            Pageable pageable) {
        return service.search(from, to, categoryId, text, accountId, pageable);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public TransactionResponse create(@Valid @RequestBody TransactionRequest r) {
        return service.create(r);
    }

    @PatchMapping("/{id}")
    public TransactionResponse changeCategory(@PathVariable Long id,
                                              @RequestBody CategoryChangeRequest r) {
        return service.changeCategory(id, r);
    }

    @PostMapping("/recategorize")
    public RecategorizeResponse recategorize() {
        return service.recategorize();
    }
}