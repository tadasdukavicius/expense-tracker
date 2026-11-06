package com.example.expensetracker.repository;

import com.example.expensetracker.dto.CategorySpendRow;
import com.example.expensetracker.entity.Transaction;

import java.time.LocalDate;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface TransactionRepository
        extends JpaRepository<Transaction, Long>, JpaSpecificationExecutor<Transaction> {

    boolean existsByAccount_IdAndDedupHash(Long accountId, String dedupHash);

    List<Transaction> findAllByManuallyCategorizedFalse();

    @Query("""
            select new com.example.expensetracker.dto.CategorySpendRow(
                c.id, c.name, c.color, c.emoji, sum(-t.amount))
            from Transaction t left join t.category c
            where t.amount < 0
              and (c.id is null or c.excludedFromStats = false)
              and t.bookingDate >= :from and t.bookingDate <= :to
              and (cast(:accountId as long) is null or t.account.id = :accountId)
            group by c.id, c.name, c.color, c.emoji
            """)
    List<CategorySpendRow> spendByCategory(@Param("from") LocalDate from,
                                           @Param("to") LocalDate to,
                                           @Param("accountId") Long accountId);
}
