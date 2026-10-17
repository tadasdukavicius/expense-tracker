package com.example.expensetracker.categorization;

import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

import com.example.expensetracker.entity.Category;
import com.example.expensetracker.entity.CategoryRule;
import com.example.expensetracker.entity.MatchField;

import org.springframework.stereotype.Component;

/**
 * Pure rule matching, with no Spring or DB access.
 * Counterparty rules are tried first, then description rules.
 * Within a field, a lower priority number wins, then a lower id.
 */
@Component
public class RuleEngine {

    private static final Comparator<CategoryRule> ORDER =
            Comparator.comparingInt(CategoryRule::getPriority)
                    .thenComparing(CategoryRule::getId, Comparator.nullsLast(Comparator.naturalOrder()));

    public Optional<Category> categorize(String description, String counterparty, List<CategoryRule> rules) {
        return firstMatch(MatchField.COUNTERPARTY, counterparty, rules)
                .or(() -> firstMatch(MatchField.DESCRIPTION, description, rules));
    }

    private Optional<Category> firstMatch(MatchField field, String value, List<CategoryRule> rules) {
        if (value == null || value.isBlank()) {
            return Optional.empty();
        }
        String haystack = normalize(value);
        return rules.stream()
                .filter(r -> r.getMatchField() == field)
                .filter(r -> r.getPattern() != null && !r.getPattern().isBlank())
                .sorted(ORDER)
                .filter(r -> haystack.contains(normalize(r.getPattern())))
                .map(CategoryRule::getCategory)
                .findFirst();
    }

    private String normalize(String s) {
        return s.trim().toLowerCase(Locale.ROOT);
    }
}