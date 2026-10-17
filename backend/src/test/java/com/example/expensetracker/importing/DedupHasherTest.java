package com.example.expensetracker.importing;

import com.example.expensetracker.importing.parser.ParsedTransaction;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;

class DedupHasherTest {

    private final DedupHasher hasher = new DedupHasher();

    @Test
    void sameTransactionWithDifferentCaseWhitespaceAndAmountScaleHasSameHash() {
        ParsedTransaction first = new ParsedTransaction(
                LocalDate.of(2026, 9, 2),
                new BigDecimal("-42.180"),
                "EUR",
                "Weekly   groceries",
                " Maxima LT ",
                "REF-1002");

        ParsedTransaction second = new ParsedTransaction(
                LocalDate.of(2026, 9, 2),
                new BigDecimal("-42.18"),
                "EUR",
                "weekly groceries",
                "maxima lt",
                "ref-1002");

        assertThat(hasher.hash(first)).isEqualTo(hasher.hash(second));
    }

    @Test
    void differentReferenceProducesDifferentHash() {
        ParsedTransaction first = new ParsedTransaction(
                LocalDate.of(2026, 9, 2), new BigDecimal("-42.18"),
                "EUR", "Weekly groceries", "Maxima", "REF-1002");

        ParsedTransaction second = new ParsedTransaction(
                LocalDate.of(2026, 9, 2), new BigDecimal("-42.18"),
                "EUR", "Weekly groceries", "Maxima", "REF-OTHER");

        assertThat(hasher.hash(first)).isNotEqualTo(hasher.hash(second));
    }

    @Test
    void nullOptionalFieldsAreHandledLikeBlankFields() {
        ParsedTransaction withNulls = new ParsedTransaction(
                LocalDate.of(2026, 9, 15), new BigDecimal("-11.60"),
                "EUR", "Household items", null, null);

        ParsedTransaction withBlanks = new ParsedTransaction(
                LocalDate.of(2026, 9, 15), new BigDecimal("-11.60"),
                "EUR", "Household items", "  ", "");

        assertThat(hasher.hash(withNulls)).isEqualTo(hasher.hash(withBlanks));
    }
}