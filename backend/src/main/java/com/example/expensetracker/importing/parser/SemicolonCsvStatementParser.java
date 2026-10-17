package com.example.expensetracker.importing.parser;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Set;

import org.springframework.stereotype.Component;

@Component
public class SemicolonCsvStatementParser implements StatementParser {

    private static final Set<String> REQUIRED_HEADERS = Set.of(
            "booking_date", "amount", "currency", "description", "counterparty", "reference");

    @Override
    public boolean supports(String filename) {
        return filename != null && filename.toLowerCase(Locale.ROOT).endsWith(".csv");
    }

    @Override
    public List<ParsedTransaction> parse(InputStream input) throws IOException {
        CSVFormat format = CSVFormat.DEFAULT.builder()
                .setDelimiter(';')
                .setHeader()
                .setSkipHeaderRecord(true)
                .get();

        try (CSVParser csv = format.parse(new InputStreamReader(input, StandardCharsets.UTF_8))) {
            Set<String> headers = csv.getHeaderMap().keySet();
            if (!headers.containsAll(REQUIRED_HEADERS)) {
                throw new IOException("CSV is missing required headers: " + REQUIRED_HEADERS);
            }

            List<ParsedTransaction> rows = new ArrayList<>();
            for (CSVRecord row : csv) {
                try {
                    rows.add(new ParsedTransaction(
                            LocalDate.parse(required(row, "booking_date")),
                            new BigDecimal(required(row, "amount")),
                            required(row, "currency").toUpperCase(Locale.ROOT),
                            required(row, "description"),
                            emptyToNull(row.get("counterparty")),
                            emptyToNull(row.get("reference"))));
                } catch (DateTimeParseException | NumberFormatException e) {
                    throw new IOException("Invalid date or amount in CSV row " + row.getRecordNumber(), e);
                }
            }
            return List.copyOf(rows);
        }
    }

    private static String required(CSVRecord row, String column) throws IOException {
        String value = row.get(column).trim();
        if (value.isEmpty()) {
            throw new IOException("Empty required value in CSV row " + row.getRecordNumber());
        }
        return value;
    }

    private static String emptyToNull(String value) {
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}