package com.example.expensetracker.repository;

import com.example.expensetracker.entity.CategoryRule;
import com.example.expensetracker.entity.MatchField;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface CategoryRuleRepository extends JpaRepository<CategoryRule, Long> {
    List<CategoryRule> findAllByOrderByPriorityAsc();

    boolean existsByMatchFieldAndPatternIgnoreCase(MatchField matchField, String pattern);
}