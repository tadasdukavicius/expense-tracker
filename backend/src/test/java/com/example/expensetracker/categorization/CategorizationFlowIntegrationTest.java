package com.example.expensetracker.categorization;

import com.example.expensetracker.TestcontainersConfiguration;
import com.example.expensetracker.entity.Account;
import com.example.expensetracker.entity.Category;
import com.example.expensetracker.entity.Transaction;
import com.example.expensetracker.repository.AccountRepository;
import com.example.expensetracker.repository.CategoryRepository;
import com.example.expensetracker.repository.CategoryRuleRepository;
import com.example.expensetracker.repository.TransactionRepository;
import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.support.TransactionTemplate;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Import(TestcontainersConfiguration.class)
class CategorizationFlowIntegrationTest {

    @Autowired MockMvc mvc;
    @Autowired AccountRepository accounts;
    @Autowired CategoryRepository categories;
    @Autowired CategoryRuleRepository rules;
    @Autowired TransactionRepository transactions;
    @Autowired TransactionTemplate tx;

    Long accountId;
    Long groceriesId;
    Long transportId;

    @BeforeEach
    void setUp() {
        transactions.deleteAll();
        rules.deleteAll();
        categories.deleteAll();

        Account account = new Account();
        account.setName("Test account");
        accountId = accounts.save(account).getId();
        groceriesId = category("Groceries");
        transportId = category("Transport");
    }

    private Long category(String name) {
        Category c = new Category();
        c.setName(name);
        return categories.save(c).getId();
    }

    private void createRule(String field, String pattern, Long categoryId, int priority) throws Exception {
        mvc.perform(post("/api/category-rules").contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"matchField":"%s","pattern":"%s","categoryId":%d,"priority":%d}
                                """.formatted(field, pattern, categoryId, priority)))
                .andExpect(status().isCreated());
    }

    private Long saveTransaction(String description, String counterparty, Long categoryId, boolean manual) {
        return tx.execute(s -> {
            Transaction t = new Transaction();
            t.setBookingDate(LocalDate.of(2026, 9, 1));
            t.setAmount(new BigDecimal("-10.00"));
            t.setCurrency("EUR");
            t.setDescription(description);
            t.setCounterparty(counterparty);
            t.setDedupHash(description + counterparty);
            t.setAccount(accounts.getReferenceById(accountId));
            if (categoryId != null) {
                t.setCategory(categories.getReferenceById(categoryId));
            }
            t.setManuallyCategorized(manual);
            return transactions.save(t).getId();
        });
    }

    private String categoryNameOf(Long transactionId) {
        return tx.execute(s -> {
            Transaction t = transactions.findById(transactionId).orElseThrow();
            return t.getCategory() == null ? null : t.getCategory().getName();
        });
    }

    @Test
    void importConfirmationAppliesRules() throws Exception {
        createRule("COUNTERPARTY", "maxima", groceriesId, 10);

        byte[] content = getClass().getResourceAsStream("/sample-statement.csv").readAllBytes();
        var file = new MockMultipartFile("file", "sample-statement.csv", "text/csv", content);
        String body = mvc.perform(multipart("/api/imports").file(file).param("accountId", accountId.toString()))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        long batchId = JsonPath.parse(body).read("$.batchId", Long.class);

        mvc.perform(post("/api/imports/{id}/confirm", batchId)).andExpect(status().isOk());

        long maxima = transactions.findAll().stream()
                .filter(t -> "Maxima".equals(t.getCounterparty())).count();
        long categorized = tx.execute(s -> transactions.findAll().stream()
                .filter(t -> t.getCategory() != null && "Groceries".equals(t.getCategory().getName())).count());
        assertThat(maxima).isPositive();
        assertThat(categorized).isEqualTo(maxima);
    }

    @Test
    void recategorizeSkipsManualOverrides() throws Exception {
        createRule("COUNTERPARTY", "maxima", groceriesId, 10);
        Long auto = saveTransaction("Shopping", "Maxima", null, false);
        Long manual = saveTransaction("Shopping", "Maxima LT", transportId, true);

        mvc.perform(post("/api/transactions/recategorize"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.examined").value(1))
                .andExpect(jsonPath("$.updated").value(1));

        assertThat(categoryNameOf(auto)).isEqualTo("Groceries");
        assertThat(categoryNameOf(manual)).isEqualTo("Transport");
    }

    @Test
    void manualPatchMarksTransactionAsManual() throws Exception {
        createRule("COUNTERPARTY", "maxima", groceriesId, 10);
        Long id = saveTransaction("Shopping", "Maxima", null, false);

        mvc.perform(patch("/api/transactions/{id}", id).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"categoryId\":" + transportId + "}"))
                .andExpect(status().isOk());
        mvc.perform(post("/api/transactions/recategorize")).andExpect(status().isOk())
                .andExpect(jsonPath("$.examined").value(0));

        assertThat(categoryNameOf(id)).isEqualTo("Transport");
    }

    @Test
    void createRuleFromTransactionUsesCounterparty() throws Exception {
        Long id = saveTransaction("Card payment", "Circle K", transportId, true);

        mvc.perform(post("/api/category-rules/from-transaction/{id}", id))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.matchField").value("COUNTERPARTY"))
                .andExpect(jsonPath("$.pattern").value("Circle K"))
                .andExpect(jsonPath("$.category.name").value("Transport"));

        mvc.perform(post("/api/category-rules/from-transaction/{id}", id))
                .andExpect(status().isConflict());
    }

    @Test
    void createRuleFromTransactionFallsBackToDescription() throws Exception {
        Long id = saveTransaction("Weekly shop", null, groceriesId, true);

        mvc.perform(post("/api/category-rules/from-transaction/{id}", id))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.matchField").value("DESCRIPTION"))
                .andExpect(jsonPath("$.pattern").value("Weekly shop"));
    }

    @Test
    void createRuleFromUncategorizedTransactionReturns409() throws Exception {
        Long id = saveTransaction("Mystery", "Unknown", null, false);

        mvc.perform(post("/api/category-rules/from-transaction/{id}", id))
                .andExpect(status().isConflict());
    }

    @Test
    void ruleCrudValidatesAndHandlesMissingCategory() throws Exception {
        mvc.perform(post("/api/category-rules").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"matchField\":\"DESCRIPTION\",\"pattern\":\" \",\"categoryId\":" + groceriesId + "}"))
                .andExpect(status().isBadRequest());

        mvc.perform(post("/api/category-rules").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"matchField\":\"DESCRIPTION\",\"pattern\":\"x\",\"categoryId\":999999}"))
                .andExpect(status().isNotFound());
    }
}
