package com.example.expensetracker.service;

import com.example.expensetracker.TestcontainersConfiguration;
import com.example.expensetracker.dto.CategorySpend;
import com.example.expensetracker.dto.CategoryStatsResponse;
import com.example.expensetracker.entity.Category;
import com.example.expensetracker.entity.Transaction;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.context.annotation.Import;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import({TestcontainersConfiguration.class, StatsService.class})
class StatsServiceDbTest {

    @Autowired StatsService service;
    @Autowired EntityManager em;

    @BeforeEach
    void setUp() {
        Category food = new Category();
        food.setName("Stats Food");
        food.setColor("#112233");
        em.persist(food);

        em.persist(tx(LocalDate.of(2026, 9, 10), "-30.00", food));
        em.persist(tx(LocalDate.of(2026, 9, 12), "-10.00", food));
        em.persist(tx(LocalDate.of(2026, 9, 15), "-10.00", null));
        em.persist(tx(LocalDate.of(2026, 9, 20), "1500.00", null));
        em.persist(tx(LocalDate.of(2026, 10, 20), "-99.00", food));

        Category transfer = new Category();
        transfer.setName("Stats Transfer");
        transfer.setExcludedFromStats(true);
        em.persist(transfer);
        em.persist(tx(LocalDate.of(2026, 9, 25), "-500.00", transfer));
        em.flush();
        em.clear();
    }

    @Test
    void sumsSpendingPerCategoryWithPercentages() {
        CategoryStatsResponse r = service.byCategory(
                LocalDate.of(2026, 9, 1), LocalDate.of(2026, 9, 30), null);

        assertThat(r.total()).isEqualByComparingTo("50.00");
        assertThat(r.categories()).extracting(CategorySpend::name)
                .containsExactly("Stats Food", "Uncategorized");
        assertThat(r.categories().get(0).amount()).isEqualByComparingTo("40.00");
        assertThat(r.categories().get(0).percent()).isEqualByComparingTo("80.0");
        assertThat(r.categories().get(1).percent()).isEqualByComparingTo("20.0");
    }

    @Test
    void openEndedRangeIncludesEverything() {
        assertThat(service.byCategory(null, null, null).total()).isEqualByComparingTo("149.00");
    }

    @Test
    void emptyRangeGivesZeroTotal() {
        CategoryStatsResponse r = service.byCategory(
                LocalDate.of(2020, 1, 1), LocalDate.of(2020, 1, 31), null);
        assertThat(r.total()).isEqualByComparingTo("0");
        assertThat(r.categories()).isEmpty();
    }

    private Transaction tx(LocalDate date, String amount, Category category) {
        Transaction t = new Transaction();
        t.setBookingDate(date);
        t.setAmount(new BigDecimal(amount));
        t.setCurrency("EUR");
        t.setDescription("x");
        t.setCategory(category);
        return t;
    }
}
