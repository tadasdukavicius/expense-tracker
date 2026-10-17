package com.example.expensetracker.repository;

import com.example.expensetracker.entity.Transaction;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface TransactionRepository
        extends JpaRepository<Transaction, Long>, JpaSpecificationExecutor<Transaction> {

        boolean existsByAccount_IdAndDedupHash(Long accountId, String dedupHash);
}