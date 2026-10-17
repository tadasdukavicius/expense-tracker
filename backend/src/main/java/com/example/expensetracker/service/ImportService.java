package com.example.expensetracker.service;

import com.example.expensetracker.dto.ImportPreviewResponse;
import com.example.expensetracker.entity.Account;
import com.example.expensetracker.entity.ImportBatch;
import com.example.expensetracker.entity.ImportBatchRow;
import com.example.expensetracker.exception.InvalidImportException;
import com.example.expensetracker.exception.NotFoundException;
import com.example.expensetracker.importing.DedupHasher;
import com.example.expensetracker.importing.parser.ParsedTransaction;
import com.example.expensetracker.importing.parser.StatementParser;
import com.example.expensetracker.repository.AccountRepository;
import com.example.expensetracker.repository.ImportBatchRepository;
import com.example.expensetracker.repository.TransactionRepository;
import com.example.expensetracker.dto.ImportResultResponse;
import com.example.expensetracker.entity.ImportBatchStatus;
import com.example.expensetracker.entity.Transaction;
import com.example.expensetracker.exception.ConflictException;
import com.example.expensetracker.categorization.RuleEngine;
import com.example.expensetracker.repository.CategoryRuleRepository;
import lombok.RequiredArgsConstructor;
import com.example.expensetracker.entity.CategoryRule;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class ImportService {

    private final AccountRepository accountRepository;
    private final ImportBatchRepository batchRepository;
    private final TransactionRepository transactionRepository;
    private final List<StatementParser> parsers;
    private final DedupHasher dedupHasher;
    private final CategoryRuleRepository ruleRepository;
    private final RuleEngine ruleEngine;

    @Transactional
    public ImportPreviewResponse preview(Long accountId, MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new InvalidImportException("Upload a non-empty CSV file");
        }

        String originalName = file.getOriginalFilename();
        if (originalName == null || originalName.isBlank()) {
            throw new InvalidImportException("Uploaded file must have a filename");
        }

        String normalizedName = originalName.replace('\\', '/');
        final String fileName = normalizedName.substring(normalizedName.lastIndexOf('/') + 1);

        StatementParser parser = parsers.stream()
                .filter(candidate -> candidate.supports(fileName))
                .findFirst()
                .orElseThrow(() -> new InvalidImportException("Unsupported file type; upload a CSV"));

        Account account = accountRepository.findById(accountId)
                .orElseThrow(() -> new NotFoundException("Account not found: " + accountId));

        final List<ParsedTransaction> parsedRows;
        try {
            parsedRows = parser.parse(file.getInputStream());
        } catch (IOException e) {
            throw new InvalidImportException("Could not parse statement: " + e.getMessage(), e);
        }

        if (parsedRows.isEmpty()) {
            throw new InvalidImportException("CSV contains no transaction rows");
        }

        ImportBatch batch = new ImportBatch();
        batch.setAccount(account);
        batch.setFileName(fileName);

        Set<String> hashesSeenInThisFile = new HashSet<>();
        List<ImportPreviewResponse.Row> previewRows = new ArrayList<>();
        int newCount = 0;
        int duplicateCount = 0;
        int rowNumber = 1;

        for (ParsedTransaction parsed : parsedRows) {
            String hash = dedupHasher.hash(parsed);
            boolean duplicate = transactionRepository
                    .existsByAccount_IdAndDedupHash(accountId, hash)
                    || !hashesSeenInThisFile.add(hash);

            ImportBatchRow row = new ImportBatchRow();
            row.setStatementRowNumber(rowNumber);
            row.setBookingDate(parsed.bookingDate());
            row.setAmount(parsed.amount());
            row.setCurrency(parsed.currency());
            row.setDescription(parsed.description());
            row.setCounterparty(parsed.counterparty());
            row.setReference(parsed.reference());
            row.setDedupHash(hash);
            row.setDuplicate(duplicate);
            batch.addRow(row);

            previewRows.add(new ImportPreviewResponse.Row(
                    rowNumber,
                    parsed.bookingDate(),
                    parsed.amount(),
                    parsed.currency(),
                    parsed.description(),
                    parsed.counterparty(),
                    parsed.reference(),
                    duplicate));

            if (duplicate) {
                duplicateCount++;
            } else {
                newCount++;
            }
            rowNumber++;
        }

        batch.setNewCount(newCount);
        batch.setDuplicateCount(duplicateCount);
        ImportBatch saved = batchRepository.save(batch);

        return new ImportPreviewResponse(
                saved.getId(),
                saved.getFileName(),
                parsedRows.size(),
                newCount,
                duplicateCount,
                List.copyOf(previewRows));
    }

    @Transactional
    public ImportResultResponse confirm(Long batchId) {
        ImportBatch batch = batchRepository.findById(batchId)
                .orElseThrow(() -> new NotFoundException("Import batch not found: " + batchId));

        if (batch.getStatus() == ImportBatchStatus.CONFIRMED) {
            throw new ConflictException("Import batch already confirmed: " + batchId);
        }

        Long accountId = batch.getAccount().getId();
        Set<String> importedHashes = new HashSet<>();
        List<Transaction> toSave = new ArrayList<>();
        int skipped = 0;

        List<CategoryRule> rules = ruleRepository.findAllByOrderByPriorityAsc();

        for (ImportBatchRow row : batch.getRows()) {
            // Re-check the database: another batch may have been confirmed since this preview.
            boolean duplicate = row.isDuplicate()
                    || !importedHashes.add(row.getDedupHash())
                    || transactionRepository.existsByAccount_IdAndDedupHash(accountId, row.getDedupHash());

            if (duplicate) {
                skipped++;
                continue;
            }

            Transaction t = new Transaction();
            t.setAccount(batch.getAccount());
            t.setBookingDate(row.getBookingDate());
            t.setAmount(row.getAmount());
            t.setCurrency(row.getCurrency());
            t.setDescription(row.getDescription());
            t.setCounterparty(row.getCounterparty());
            t.setReference(row.getReference());
            t.setDedupHash(row.getDedupHash());
            ruleEngine.categorize(row.getDescription(), row.getCounterparty(), rules)
                    .ifPresent(t::setCategory);
            toSave.add(t);
        }

        transactionRepository.saveAll(toSave);

        batch.setNewCount(toSave.size());
        batch.setDuplicateCount(skipped);
        batch.setStatus(ImportBatchStatus.CONFIRMED);

        return new ImportResultResponse(batch.getId(), batch.getFileName(),
                batch.getStatus(), toSave.size(), skipped);
    }
}