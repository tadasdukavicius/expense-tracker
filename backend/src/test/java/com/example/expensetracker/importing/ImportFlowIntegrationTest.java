package com.example.expensetracker.importing;

import com.example.expensetracker.TestcontainersConfiguration;
import com.example.expensetracker.entity.Account;
import com.example.expensetracker.repository.AccountRepository;
import com.example.expensetracker.repository.TransactionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Import(TestcontainersConfiguration.class)
class ImportFlowIntegrationTest {

    @Autowired MockMvc mvc;
    @Autowired AccountRepository accounts;
    @Autowired TransactionRepository transactions;

    Long accountId;

    @BeforeEach
    void setUp() {
        transactions.deleteAll();
        Account account = new Account();
        account.setName("Test account");
        accountId = accounts.save(account).getId();
    }

    private MockMultipartFile sampleFile() throws Exception {
        byte[] content = getClass().getResourceAsStream("/sample-statement.csv").readAllBytes();
        return new MockMultipartFile("file", "sample-statement.csv", "text/csv", content);
    }

    private long previewBatchId() throws Exception {
        MvcResult result = mvc.perform(multipart("/api/imports")
                        .file(sampleFile()).param("accountId", accountId.toString()))
                .andExpect(status().isOk())
                .andReturn();
        String body = result.getResponse().getContentAsString();
        return com.jayway.jsonpath.JsonPath.parse(body).read("$.batchId", Long.class);
    }

    @Test
    void previewFlagsDuplicateInsideTheFile() throws Exception {
        mvc.perform(multipart("/api/imports")
                        .file(sampleFile()).param("accountId", accountId.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalRows").value(15))
                .andExpect(jsonPath("$.newCount").value(14))
                .andExpect(jsonPath("$.duplicateCount").value(1));
        assertThat(transactions.count()).isZero(); // preview must not save transactions
    }

    @Test
    void confirmSavesOnlyNewRows() throws Exception {
        long batchId = previewBatchId();

        mvc.perform(post("/api/imports/{id}/confirm", batchId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.importedCount").value(14))
                .andExpect(jsonPath("$.skippedCount").value(1))
                .andExpect(jsonPath("$.status").value("CONFIRMED"));

        assertThat(transactions.count()).isEqualTo(14);
    }

    @Test
    void confirmingTwiceReturns409() throws Exception {
        long batchId = previewBatchId();
        mvc.perform(post("/api/imports/{id}/confirm", batchId)).andExpect(status().isOk());

        mvc.perform(post("/api/imports/{id}/confirm", batchId))
                .andExpect(status().isConflict());
    }

    @Test
    void importingSameFileTwiceYieldsNoNewRows() throws Exception {
        long first = previewBatchId();
        mvc.perform(post("/api/imports/{id}/confirm", first)).andExpect(status().isOk());

        mvc.perform(multipart("/api/imports")
                        .file(sampleFile()).param("accountId", accountId.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.newCount").value(0))
                .andExpect(jsonPath("$.duplicateCount").value(15));
    }

    @Test
    void unknownExtensionReturns400() throws Exception {
        MockMultipartFile txt = new MockMultipartFile(
                "file", "statement.txt", "text/plain", "hello".getBytes(StandardCharsets.UTF_8));

        mvc.perform(multipart("/api/imports").file(txt).param("accountId", accountId.toString()))
                .andExpect(status().isBadRequest());
    }

    @Test
    void malformedCsvReturns400() throws Exception {
        String csv = "booking_date;amount;currency;description;counterparty;reference\n"
                + "not-a-date;bad;EUR;Coffee;Cafe;REF-1\n";
        MockMultipartFile bad = new MockMultipartFile(
                "file", "bad.csv", "text/csv", csv.getBytes(StandardCharsets.UTF_8));

        mvc.perform(multipart("/api/imports").file(bad).param("accountId", accountId.toString()))
                .andExpect(status().isBadRequest());
    }

    @Test
    void unknownAccountReturns404() throws Exception {
        mvc.perform(multipart("/api/imports").file(sampleFile()).param("accountId", "999999"))
                .andExpect(status().isNotFound());
    }
}