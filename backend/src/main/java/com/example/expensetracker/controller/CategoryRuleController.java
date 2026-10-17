package com.example.expensetracker.controller;

import com.example.expensetracker.dto.CategoryRuleRequest;
import com.example.expensetracker.dto.CategoryRuleResponse;
import com.example.expensetracker.service.CategoryRuleService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/category-rules")
@RequiredArgsConstructor
public class CategoryRuleController {

    private final CategoryRuleService service;

    @GetMapping
    public List<CategoryRuleResponse> list() {
        return service.list();
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public CategoryRuleResponse create(@Valid @RequestBody CategoryRuleRequest r) {
        return service.create(r);
    }

    @PostMapping("/from-transaction/{transactionId}")
    @ResponseStatus(HttpStatus.CREATED)
    public CategoryRuleResponse createFromTransaction(@PathVariable Long transactionId) {
        return service.createFromTransaction(transactionId);
    }

    @PutMapping("/{id}")
    public CategoryRuleResponse update(@PathVariable Long id, @Valid @RequestBody CategoryRuleRequest r) {
        return service.update(id, r);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long id) {
        service.delete(id);
    }
}
