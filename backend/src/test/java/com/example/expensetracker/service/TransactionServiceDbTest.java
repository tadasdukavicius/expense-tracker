package com.example.expensetracker.service;

import com.example.expensetracker.TestcontainersConfiguration;
import com.example.expensetracker.categorization.RuleEngine;
import com.example.expensetracker.dto.TransactionResponse;
import com.example.expensetracker.entity.Category;
import com.example.expensetracker.entity.Transaction;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.PageRequest;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import({TestcontainersConfiguration.class, TransactionService.class, RuleEngine.class})
class TransactionServiceDbTest {

    @Autowired TransactionService service;
    @Autowired EntityManager em;

    Category groceries;

    @BeforeEach
    void setUp() {
        groceries = new Category();
        groceries.setName("Groceries");
        em.persist(groceries);

        em.persist(tx(LocalDate.of(2026, 9, 10), "-12.30", "MAXIMA LT", groceries));
        em.persist(tx(LocalDate.of(2026, 10, 2), "-45.00", "Rent October", null));
        em.persist(tx(LocalDate.of(2026, 10, 5), "1500.00", "Salary", null));
        em.flush();
        em.clear();
    }

    @Test
    void noFiltersReturnsEverything() {
        assertThat(search(null, null, null, null)).hasSize(3);
    }

    @Test
    void filtersByDateRange() {
        var result = search(LocalDate.of(2026, 10, 1), LocalDate.of(2026, 10, 31), null, null);
        assertThat(result).extracting(TransactionResponse::description)
                .containsExactlyInAnyOrder("Rent October", "Salary");
    }

    @Test
    void filtersByCategory() {
        var result = search(null, null, groceries.getId(), null);
        assertThat(result).extracting(TransactionResponse::description).containsExactly("MAXIMA LT");
    }

    @Test
    void filtersByTextIgnoringCase() {
        var result = search(null, null, null, "maxima");
        assertThat(result).hasSize(1);
    }

    @Test
    void combinesFilters() {
        var result = search(LocalDate.of(2026, 10, 1), null, groceries.getId(), null);
        assertThat(result).isEmpty();
    }

    @Test
    void amountKeepsExactDecimalValue() {
        var result = search(null, null, groceries.getId(), null);
        assertThat(result.get(0).amount()).isEqualTo(new BigDecimal("-12.30"));
    }

    private java.util.List<TransactionResponse> search(LocalDate from, LocalDate to, Long categoryId, String text) {
        return service.search(from, to, categoryId, text, PageRequest.of(0, 10)).getContent();
    }

    private Transaction tx(LocalDate date, String amount, String description, Category category) {
        Transaction t = new Transaction();
        t.setBookingDate(date);
        t.setAmount(new BigDecimal(amount));
        t.setCurrency("EUR");
        t.setDescription(description);
        t.setCategory(category);
        return t;
    }
}