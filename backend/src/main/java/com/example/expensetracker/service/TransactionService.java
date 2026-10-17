package com.example.expensetracker.service;

import com.example.expensetracker.dto.CategoryChangeRequest;
import com.example.expensetracker.dto.TransactionRequest;
import com.example.expensetracker.dto.TransactionResponse;
import com.example.expensetracker.entity.Category;
import com.example.expensetracker.entity.Transaction;
import com.example.expensetracker.entity.CategoryRule;
import com.example.expensetracker.exception.NotFoundException;
import com.example.expensetracker.mapper.Mapper;
import com.example.expensetracker.repository.CategoryRepository;
import com.example.expensetracker.repository.TransactionRepository;
import lombok.RequiredArgsConstructor;
import com.example.expensetracker.dto.RecategorizeResponse;

import com.example.expensetracker.categorization.RuleEngine;
import com.example.expensetracker.repository.CategoryRuleRepository;

import java.util.List;
import java.util.Objects;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;

@Service
@RequiredArgsConstructor
public class TransactionService {

    private final TransactionRepository repo;
    private final CategoryRepository categoryRepo;
    private final CategoryRuleRepository ruleRepo;
    private final RuleEngine ruleEngine;

    @Transactional(readOnly = true)
    public Page<TransactionResponse> search(LocalDate from, LocalDate to, Long categoryId,
                                            String text, Pageable pageable) {
        Specification<Transaction> spec = (r, q, cb) -> cb.conjunction();
        if (from != null) {
            spec = spec.and((r, q, cb) -> cb.greaterThanOrEqualTo(r.get("bookingDate"), from));
        }
        if (to != null) {
            spec = spec.and((r, q, cb) -> cb.lessThanOrEqualTo(r.get("bookingDate"), to));
        }
        if (categoryId != null) {
            spec = spec.and((r, q, cb) -> cb.equal(r.get("category").get("id"), categoryId));
        }
        if (text != null && !text.isBlank()) {
            String like = "%" + text.toLowerCase() + "%";
            spec = spec.and((r, q, cb) -> cb.like(cb.lower(r.get("description")), like));
        }
        return repo.findAll(spec, pageable).map(Mapper::toResponse);
    }

    @Transactional
    public TransactionResponse create(TransactionRequest r) {
        Transaction t = new Transaction();
        t.setBookingDate(r.bookingDate());
        t.setAmount(r.amount());
        t.setCurrency(r.currency().toUpperCase());
        t.setDescription(r.description());
        t.setCounterparty(r.counterparty());
        t.setCategory(r.categoryId() == null ? null : findCategory(r.categoryId()));
        return Mapper.toResponse(repo.save(t));
    }

    @Transactional
    public TransactionResponse changeCategory(Long id, CategoryChangeRequest r) {
        Transaction t = repo.findById(id)
                .orElseThrow(() -> new NotFoundException("Transaction not found: " + id));
        Category category = r.categoryId() == null ? null : findCategory(r.categoryId());
        t.setCategory(category);
        t.setManuallyCategorized(category != null);
        return Mapper.toResponse(t);
    }

    private Category findCategory(Long id) {
        return categoryRepo.findById(id)
                .orElseThrow(() -> new NotFoundException("Category not found: " + id));
    }

    @Transactional
    public RecategorizeResponse recategorize() {
        List<CategoryRule> rules = ruleRepo.findAllByOrderByPriorityAsc();
        List<Transaction> candidates = repo.findAllByManuallyCategorizedFalse();
        int updated = 0;
        for (Transaction t : candidates) {
            Category match = ruleEngine.categorize(t.getDescription(), t.getCounterparty(), rules)
                    .orElse(null);
            if (match != null && !Objects.equals(match.getId(), t.getCategory() == null ? null : t.getCategory().getId())) {
                t.setCategory(match);
                updated++;
            }
        }
        return new RecategorizeResponse(candidates.size(), updated);
    }
}