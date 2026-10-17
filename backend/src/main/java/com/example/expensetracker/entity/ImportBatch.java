package com.example.expensetracker.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "import_batch")
@Getter
@Setter
@NoArgsConstructor
public class ImportBatch {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "account_id", nullable = false)
    private Account account;

    @Column(name = "file_name", nullable = false, length = 255)
    private String fileName;

    @Column(name = "imported_at", nullable = false)
    private Instant importedAt = Instant.now();

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private ImportBatchStatus status = ImportBatchStatus.PENDING;

    @Column(name = "new_count", nullable = false)
    private int newCount;

    @Column(name = "duplicate_count", nullable = false)
    private int duplicateCount;

    @OneToMany(mappedBy = "importBatch", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("statementRowNumber") 
    private List<ImportBatchRow> rows = new ArrayList<>();

    public void addRow(ImportBatchRow row) {
        rows.add(row);
        row.setImportBatch(this);
    }
}