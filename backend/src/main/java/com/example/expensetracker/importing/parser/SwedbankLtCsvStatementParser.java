package com.example.expensetracker.importing.parser;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.math.BigDecimal;
import java.nio.ByteBuffer;
import java.nio.charset.CharacterCodingException;
import java.nio.charset.Charset;
import java.nio.charset.CodingErrorAction;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Swedbank Lithuania export. Columns by position: 0 account, 1 row type (10 = balance,
 * 20 = transaction), 2 date, 3 counterparty, 4 description, 5 amount (always positive),
 * 6 currency, 7 D/K (D = debit, K = credit), 8 record number. Remaining columns are ignored.
 */
@Component
public class SwedbankLtCsvStatementParser implements StatementParser {

    private static final String TRANSACTION_ROW = "20";
    private static final Charset WINDOWS_1257 = Charset.forName("windows-1257");

    @Override
    public boolean supports(String filename) {
        return filename != null && filename.toLowerCase(Locale.ROOT).endsWith(".csv");
    }

    @Override
    public boolean recognizes(String firstLine) {
        return firstLine.contains("D/K") && firstLine.contains("Suma");
    }

    @Override
    public List<ParsedTransaction> parse(InputStream input) throws IOException {
        String text = decode(input.readAllBytes());
        String header = text.lines().findFirst().orElse("");
        CSVFormat format = CSVFormat.DEFAULT.builder()
                .setDelimiter(detectDelimiter(header))
                .setIgnoreEmptyLines(true)
                .get();

        List<ParsedTransaction> rows = new ArrayList<>();
        try (CSVParser csv = format.parse(new java.io.StringReader(text))) {
            boolean first = true;
            for (CSVRecord row : csv) {
                if (first) {
                    first = false;
                    continue;
                }
                if (row.size() < 8 || !TRANSACTION_ROW.equals(row.get(1).trim())) {
                    continue;
                }
                rows.add(toTransaction(row));
            }
        }
        return List.copyOf(rows);
    }

    private static ParsedTransaction toTransaction(CSVRecord row) throws IOException {
        try {
            BigDecimal amount = new BigDecimal(normalizeNumber(row.get(5)));
            String direction = row.get(7).trim().toUpperCase(Locale.ROOT);
            if (direction.equals("D")) {
                amount = amount.abs().negate();
            } else if (direction.equals("K")) {
                amount = amount.abs();
            } else {
                throw new IOException("Unknown D/K value '" + direction + "' in row " + row.getRecordNumber());
            }
            String counterparty = blankToNull(row.get(3));
            String description = blankToNull(row.get(4));
            if (description == null) {
                description = counterparty != null ? counterparty : "(no description)";
            }
            return new ParsedTransaction(
                    LocalDate.parse(row.get(2).trim()),
                    amount,
                    row.get(6).trim().toUpperCase(Locale.ROOT),
                    description,
                    counterparty,
                    row.size() > 8 ? blankToNull(row.get(8)) : null);
        } catch (DateTimeParseException | NumberFormatException e) {
            throw new IOException("Invalid date or amount in row " + row.getRecordNumber(), e);
        }
    }

    private static String normalizeNumber(String value) {
        String v = value.trim().replace(" ", "").replace("\u00a0", "");
        return v.contains(",") && !v.contains(".") ? v.replace(',', '.') : v;
    }

    private static char detectDelimiter(String header) {
        int semicolons = count(header, ';');
        int tabs = count(header, '\t');
        int commas = count(header, ',');
        if (semicolons >= tabs && semicolons >= commas && semicolons > 0) return ';';
        return tabs >= commas && tabs > 0 ? '\t' : ',';
    }

    private static int count(String s, char c) {
        return (int) s.chars().filter(ch -> ch == c).count();
    }

    private static String decode(byte[] bytes) {
        try {
            String text = StandardCharsets.UTF_8.newDecoder()
                    .onMalformedInput(CodingErrorAction.REPORT)
                    .onUnmappableCharacter(CodingErrorAction.REPORT)
                    .decode(ByteBuffer.wrap(bytes)).toString();
            return text.startsWith("\uFEFF") ? text.substring(1) : text;
        } catch (CharacterCodingException e) {
            return new String(bytes, WINDOWS_1257);
        }
    }

    private static String blankToNull(String value) {
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}
