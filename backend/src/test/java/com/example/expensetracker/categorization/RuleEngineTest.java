package com.example.expensetracker.categorization;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;

import com.example.expensetracker.entity.Category;
import com.example.expensetracker.entity.CategoryRule;
import com.example.expensetracker.entity.MatchField;

class RuleEngineTest {

    private final RuleEngine engine = new RuleEngine();

    private static CategoryRule rule(Long id, MatchField field, String pattern, String category, int priority) {
        Category c = new Category();
        c.setName(category);
        CategoryRule r = new CategoryRule();
        r.setId(id);
        r.setMatchField(field);
        r.setPattern(pattern);
        r.setCategory(c);
        r.setPriority(priority);
        return r;
    }

    private static String name(java.util.Optional<Category> c) {
        return c.map(Category::getName).orElse(null);
    }

    @Test
    void matchesCounterpartySubstring() {
        var rules = List.of(rule(1L, MatchField.COUNTERPARTY, "maxima", "Groceries", 10));
        assertThat(name(engine.categorize("card payment", "MAXIMA LT 123", rules))).isEqualTo("Groceries");
    }

    @Test
    void matchesCaseInsensitively() {
        var rules = List.of(rule(1L, MatchField.COUNTERPARTY, "MAXIMA", "Groceries", 10));
        assertThat(name(engine.categorize("x", "maxima lt", rules))).isEqualTo("Groceries");
    }

    @Test
    void fallsBackToDescriptionWhenCounterpartyDoesNotMatch() {
        var rules = List.of(rule(1L, MatchField.DESCRIPTION, "fuel", "Gas", 10));
        assertThat(name(engine.categorize("Fuel purchase", "Unknown Shop", rules))).isEqualTo("Gas");
    }

    @Test
    void counterpartyRuleBeatsDescriptionRuleEvenWithWorsePriority() {
        var rules = List.of(
                rule(1L, MatchField.DESCRIPTION, "fuel", "Gas", 1),
                rule(2L, MatchField.COUNTERPARTY, "circle k", "Transport", 99));
        assertThat(name(engine.categorize("fuel", "Circle K", rules))).isEqualTo("Transport");
    }

    @Test
    void descriptionRuleIgnoresCounterpartyText() {
        var rules = List.of(rule(1L, MatchField.DESCRIPTION, "maxima", "Groceries", 10));
        assertThat(engine.categorize("something", "Maxima", rules)).isEmpty();
    }

    @Test
    void noMatchReturnsEmpty() {
        var rules = List.of(rule(1L, MatchField.COUNTERPARTY, "maxima", "Groceries", 10));
        assertThat(engine.categorize("a", "Rimi", rules)).isEmpty();
    }

    @Test
    void emptyRuleListReturnsEmpty() {
        assertThat(engine.categorize("a", "b", List.of())).isEmpty();
    }

    @Test
    void lowerPriorityNumberWinsRegardlessOfListOrder() {
        var rules = List.of(
                rule(1L, MatchField.COUNTERPARTY, "max", "Low", 10),
                rule(2L, MatchField.COUNTERPARTY, "maxima", "High", 1));
        assertThat(name(engine.categorize("x", "Maxima", rules))).isEqualTo("High");
    }

    @Test
    void samePriorityLowerIdWins() {
        var rules = List.of(
                rule(5L, MatchField.COUNTERPARTY, "maxima", "Second", 10),
                rule(2L, MatchField.COUNTERPARTY, "maxima", "First", 10));
        assertThat(name(engine.categorize("x", "Maxima", rules))).isEqualTo("First");
    }

    @Test
    void nullCounterpartyDoesNotThrow() {
        var rules = List.of(
                rule(1L, MatchField.COUNTERPARTY, "maxima", "Groceries", 1),
                rule(2L, MatchField.DESCRIPTION, "coffee", "Eating out", 2));
        assertThat(name(engine.categorize("Coffee", null, rules))).isEqualTo("Eating out");
    }

    @Test
    void nullDescriptionAndCounterpartyReturnEmpty() {
        var rules = List.of(rule(1L, MatchField.DESCRIPTION, "coffee", "Eating out", 1));
        assertThat(engine.categorize(null, null, rules)).isEmpty();
    }

    @Test
    void blankPatternNeverMatches() {
        var rules = List.of(rule(1L, MatchField.COUNTERPARTY, "  ", "Everything", 1));
        assertThat(engine.categorize("a", "anything", rules)).isEmpty();
    }

    @Test
    void doesNotReorderInputList() {
        var rules = new ArrayList<>(List.of(
                rule(1L, MatchField.COUNTERPARTY, "a", "A", 50),
                rule(2L, MatchField.COUNTERPARTY, "b", "B", 1)));
        engine.categorize("x", "ab", rules);
        assertThat(rules).extracting(CategoryRule::getId).containsExactly(1L, 2L);
    }
}