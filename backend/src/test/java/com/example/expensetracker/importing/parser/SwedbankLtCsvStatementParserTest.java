package com.example.expensetracker.importing.parser;

import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class SwedbankLtCsvStatementParserTest {

    private final SwedbankLtCsvStatementParser parser = new SwedbankLtCsvStatementParser();

    @Test
    void parsesFixtureSkippingBalanceRowsAndSigningByDebitCredit() throws Exception {
        try (InputStream input = getClass().getResourceAsStream("/sample-swedbank-lt.csv")) {
            assertThat(input).isNotNull();

            var rows = parser.parse(input);

            assertThat(rows).hasSize(3);
            assertThat(rows.get(0).bookingDate()).isEqualTo(LocalDate.of(2026, 1, 1));
            assertThat(rows.get(0).amount()).isEqualByComparingTo("-8.99");
            assertThat(rows.get(0).counterparty()).isEqualTo("Spotify Example Stockholm");
            assertThat(rows.get(0).reference()).isEqualTo("2026010100000001");
            assertThat(rows.get(1).amount()).isEqualByComparingTo("-12.40");
            assertThat(rows.get(2).amount()).isEqualByComparingTo("1500.00");
        }
    }

    @Test
    void recognizesOnlyTheSwedbankHeader() {
        assertThat(parser.recognizes("Sąskaitos Nr.;;Data;Gavėjas;Paaiškinimai;Suma;Valiuta;D/K")).isTrue();
        assertThat(parser.recognizes("booking_date;amount;currency")).isFalse();
    }

    @Test
    void rejectsUnknownDebitCreditMarker() {
        String csv = "Nr;Tipas;Data;Gavejas;Paaiskinimai;Suma;Valiuta;D/K;Id\n"
                + "acc;20;2026-01-02;Shop;Card payment;5.00;EUR;X;1\n";
        assertThatThrownBy(() -> parser.parse(new ByteArrayInputStream(csv.getBytes(StandardCharsets.UTF_8))))
                .hasMessageContaining("D/K");
    }
}
