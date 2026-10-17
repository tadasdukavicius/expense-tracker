package com.example.expensetracker.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;

@Entity
@Table(
        name = "import_batch_row",
        uniqueConstraints = @UniqueConstraint(
                name = "uq_import_batch_row_number",
                columnNames = {"import_batch_id", "statement_row_number"}))
@Getter
@Setter
@NoArgsConstructor
public class ImportBatchRow {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "import_batch_id", nullable = false)
    private ImportBatch importBatch;

    @Column(name = "statement_row_number", nullable = false)
    private int statementRowNumber;

    @Column(name = "booking_date", nullable = false)
    private LocalDate bookingDate;

    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal amount;

    @Column(nullable = false, length = 3)
    private String currency;

    @Column(nullable = false, length = 500)
    private String description;

    @Column(length = 255)
    private String counterparty;

    @Column(length = 255)
    private String reference;

    @Column(name = "dedup_hash", nullable = false, length = 64)
    private String dedupHash;

    @Column(name = "is_duplicate", nullable = false)
    private boolean duplicate;
}