package com.example.expensetracker.importing.parser;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;

public interface StatementParser {

    boolean supports(String filename);

    /** Content sniffing on the first line, used when several parsers accept the same file extension. */
    default boolean recognizes(String firstLine) {
        return true;
    }

    List<ParsedTransaction> parse(InputStream input) throws IOException;
}