package com.example.expensetracker.importing;

import com.example.expensetracker.importing.parser.ParsedTransaction;
import org.springframework.stereotype.Component;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.text.Normalizer;
import java.util.HexFormat;
import java.util.Locale;
import java.util.Objects;

@Component
public final class DedupHasher {

    public String hash(ParsedTransaction transaction) {
        Objects.requireNonNull(transaction, "transaction");

        MessageDigest digest;
        try {
            digest = MessageDigest.getInstance("SHA-256");
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 is not available", e);
        }

        update(digest, transaction.bookingDate().toString());
        update(digest, transaction.amount().stripTrailingZeros().toPlainString());
        update(digest, normalizeText(transaction.counterparty()));
        update(digest, normalizeText(transaction.description()));
        update(digest, normalizeText(transaction.reference()));

        return HexFormat.of().formatHex(digest.digest());
    }

    private static String normalizeText(String value) {
        if (value == null) {
            return "";
        }
        return Normalizer.normalize(value, Normalizer.Form.NFKC)
                .trim()
                .replaceAll("\\s+", " ")
                .toLowerCase(Locale.ROOT);
    }

    private static void update(MessageDigest digest, String value) {
        byte[] bytes = value.getBytes(StandardCharsets.UTF_8);

        // Prefix each value's byte length so separator characters in text cannot
        // make two different sets of fields produce the same input sequence.
        digest.update(ByteBuffer.allocate(Integer.BYTES).putInt(bytes.length).array());
        digest.update(bytes);
    }
}