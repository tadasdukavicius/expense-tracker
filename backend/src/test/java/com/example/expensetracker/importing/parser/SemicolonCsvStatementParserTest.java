package com.example.expensetracker.importing.parser;

import org.junit.jupiter.api.Test;

import java.io.InputStream;
import java.math.BigDecimal;
import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class SemicolonCsvStatementParserTest {

    private final SemicolonCsvStatementParser parser = new SemicolonCsvStatementParser();

    @Test
    void parsesFixtureRows() throws Exception {
        try (InputStream input = getClass().getResourceAsStream("/sample-statement.csv")) {
            assertThat(input).isNotNull();

            var rows = parser.parse(input);

            assertThat(rows).hasSize(15);
            assertThat(rows.getFirst().bookingDate()).isEqualTo(LocalDate.of(2026, 9, 1));
            assertThat(rows.getFirst().amount()).isEqualByComparingTo(new BigDecimal("-8.45"));
            assertThat(rows.getFirst().counterparty()).isEqualTo("Coffee House");
            assertThat(rows.getLast().reference()).isNull();
        }
    }

    @Test
    void supportsCsvCaseInsensitively() {
        assertThat(parser.supports("statement.CSV")).isTrue();
        assertThat(parser.supports("statement.txt")).isFalse();
    }

    @Test
    void rejectsInvalidDateOrAmount() {
        String csv = "booking_date;amount;currency;description;counterparty;reference\n"
                + "not-a-date;bad;EUR;Coffee;Cafe;REF-1\n";

        assertThatThrownBy(() -> parser.parse(
                new java.io.ByteArrayInputStream(csv.getBytes(java.nio.charset.StandardCharsets.UTF_8))))
                .isInstanceOf(java.io.IOException.class)
                .hasMessageContaining("CSV row 1");
    }
}