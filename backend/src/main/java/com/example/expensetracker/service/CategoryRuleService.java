package com.example.expensetracker.service;

import com.example.expensetracker.dto.CategoryRuleRequest;
import com.example.expensetracker.dto.CategoryRuleResponse;
import com.example.expensetracker.entity.Category;
import com.example.expensetracker.entity.CategoryRule;
import com.example.expensetracker.entity.MatchField;
import com.example.expensetracker.entity.Transaction;
import com.example.expensetracker.exception.ConflictException;
import com.example.expensetracker.exception.NotFoundException;
import com.example.expensetracker.mapper.Mapper;
import com.example.expensetracker.repository.CategoryRepository;
import com.example.expensetracker.repository.CategoryRuleRepository;
import com.example.expensetracker.repository.TransactionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class CategoryRuleService {

    static final int DEFAULT_PRIORITY = 100;
    private static final int MAX_PATTERN_LENGTH = 255;

    private final CategoryRuleRepository repo;
    private final CategoryRepository categoryRepo;
    private final TransactionRepository transactionRepo;

    @Transactional(readOnly = true)
    public List<CategoryRuleResponse> list() {
        return repo.findAllByOrderByPriorityAsc().stream().map(Mapper::toResponse).toList();
    }

    @Transactional
    public CategoryRuleResponse create(CategoryRuleRequest r) {
        String pattern = r.pattern().trim();
        if (repo.existsByMatchFieldAndPatternIgnoreCase(r.matchField(), pattern)) {
            throw new ConflictException("Rule already exists: " + r.matchField() + " contains '" + pattern + "'");
        }
        CategoryRule rule = new CategoryRule();
        apply(rule, r, pattern);
        return Mapper.toResponse(repo.save(rule));
    }

    @Transactional
    public CategoryRuleResponse update(Long id, CategoryRuleRequest r) {
        CategoryRule rule = repo.findById(id)
                .orElseThrow(() -> new NotFoundException("Rule not found: " + id));
        String pattern = r.pattern().trim();
        boolean changedKey = rule.getMatchField() != r.matchField()
                || !rule.getPattern().equalsIgnoreCase(pattern);
        if (changedKey && repo.existsByMatchFieldAndPatternIgnoreCase(r.matchField(), pattern)) {
            throw new ConflictException("Rule already exists: " + r.matchField() + " contains '" + pattern + "'");
        }
        apply(rule, r, pattern);
        return Mapper.toResponse(rule);
    }

    @Transactional
    public void delete(Long id) {
        if (!repo.existsById(id)) {
            throw new NotFoundException("Rule not found: " + id);
        }
        repo.deleteById(id);
    }

    /**
     * Learns from a manual categorization: prefers the counterparty (the merchant) and
     * falls back to the description when there is none.
     */
    @Transactional
    public CategoryRuleResponse createFromTransaction(Long transactionId) {
        Transaction t = transactionRepo.findById(transactionId)
                .orElseThrow(() -> new NotFoundException("Transaction not found: " + transactionId));
        if (t.getCategory() == null) {
            throw new ConflictException("Transaction has no category to build a rule from: " + transactionId);
        }

        boolean hasCounterparty = t.getCounterparty() != null && !t.getCounterparty().isBlank();
        MatchField field = hasCounterparty ? MatchField.COUNTERPARTY : MatchField.DESCRIPTION;
        String source = hasCounterparty ? t.getCounterparty() : t.getDescription();
        String pattern = source.trim();
        if (pattern.length() > MAX_PATTERN_LENGTH) {
            pattern = pattern.substring(0, MAX_PATTERN_LENGTH);
        }
        if (repo.existsByMatchFieldAndPatternIgnoreCase(field, pattern)) {
            throw new ConflictException("Rule already exists: " + field + " contains '" + pattern + "'");
        }

        CategoryRule rule = new CategoryRule();
        rule.setMatchField(field);
        rule.setPattern(pattern);
        rule.setCategory(t.getCategory());
        rule.setPriority(DEFAULT_PRIORITY);
        return Mapper.toResponse(repo.save(rule));
    }

    private void apply(CategoryRule rule, CategoryRuleRequest r, String pattern) {
        Category category = categoryRepo.findById(r.categoryId())
                .orElseThrow(() -> new NotFoundException("Category not found: " + r.categoryId()));
        rule.setMatchField(r.matchField());
        rule.setPattern(pattern);
        rule.setCategory(category);
        rule.setPriority(r.priority() == null ? DEFAULT_PRIORITY : r.priority());
    }
}
