package com.example.expensetracker.importing.parser;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;

public interface StatementParser {

    boolean supports(String filename);

    List<ParsedTransaction> parse(InputStream input) throws IOException;
}