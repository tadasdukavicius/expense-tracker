package com.example.expensetracker.service;

import com.example.expensetracker.dto.CategoryChangeRequest;
import com.example.expensetracker.dto.TransactionRequest;
import com.example.expensetracker.dto.TransactionResponse;
import com.example.expensetracker.entity.Category;
import com.example.expensetracker.entity.Transaction;
import com.example.expensetracker.exception.NotFoundException;
import com.example.expensetracker.mapper.Mapper;
import com.example.expensetracker.repository.CategoryRepository;
import com.example.expensetracker.repository.TransactionRepository;
import lombok.RequiredArgsConstructor;
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
        t.setCategory(r.categoryId() == null ? null : findCategory(r.categoryId()));
        return Mapper.toResponse(t);
    }

    private Category findCategory(Long id) {
        return categoryRepo.findById(id)
                .orElseThrow(() -> new NotFoundException("Category not found: " + id));
    }
}