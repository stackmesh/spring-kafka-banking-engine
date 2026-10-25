package com.banking.engine.transfer;

import static java.util.concurrent.TimeUnit.SECONDS;
import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.banking.engine.account.AccountRepository;
import com.banking.engine.ledger.LedgerEntryRepository;
import com.banking.engine.ledger.PostingReason;
import com.jayway.jsonpath.JsonPath;
import java.time.Duration;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIf;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.testcontainers.DockerClientFactory;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.kafka.KafkaContainer;
import org.testcontainers.postgresql.PostgreSQLContainer;

@SpringBootTest
@ActiveProfiles("test")
@AutoConfigureMockMvc
@Testcontainers(disabledWithoutDocker = true)
@EnabledIf("dockerAvailable")
class TransferFlowTest {

    @Container
    @ServiceConnection
    static final PostgreSQLContainer postgres = new PostgreSQLContainer("postgres:16-alpine");

    @Container
    @ServiceConnection
    static final KafkaContainer kafka = new KafkaContainer("apache/kafka-native:4.1.0");

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private AccountRepository accounts;

    @Autowired
    private TransferRepository transfers;

    @Autowired
    private LedgerEntryRepository ledger;

    public static boolean dockerAvailable() {
        try {
            return DockerClientFactory.instance().isDockerAvailable();
        } catch (Throwable ex) {
            return false;
        }
    }

    @Test
    void completedTransferMovesMoneyAndWritesTheLedger() throws Exception {
        UUID source = openAccount("Ada Lovelace", "1000.00");
        UUID destination = openAccount("Grace Hopper", "0.00");

        UUID transferId = submit(source, destination, "25.50", UUID.randomUUID().toString());

        awaitStatus(transferId, TransferStatus.COMPLETED);
        assertThat(balance(source)).isEqualByComparingTo("974.50");
        assertThat(balance(destination)).isEqualByComparingTo("25.50");
        assertThat(ledger.findByAccountIdOrderByCreatedAtAsc(source))
                .extracting("postingReason")
                .containsExactly(PostingReason.OPENING_BALANCE, PostingReason.TRANSFER_DEBIT);
        assertThat(ledger.findByAccountIdOrderByCreatedAtAsc(destination))
                .extracting("postingReason")
                .containsExactly(PostingReason.TRANSFER_CREDIT);
    }

    @Test
    void clearingDeclineRollsTheDebitBack() throws Exception {
        UUID source = openAccount("Katherine Johnson", "100.00");
        UUID destination = openAccount("Margaret Hamilton", "0.00");

        UUID transferId = submit(source, destination, "10.13", UUID.randomUUID().toString());

        Transfer transfer = awaitStatus(transferId, TransferStatus.ROLLED_BACK);
        assertThat(transfer.getFailureCode()).isEqualTo("CLEARING_REJECTED");
        assertThat(balance(source)).isEqualByComparingTo("100.00");
        assertThat(balance(destination)).isEqualByComparingTo("0.00");
        assertThat(ledger.findByAccountIdOrderByCreatedAtAsc(source))
                .extracting("postingReason")
                .containsExactly(PostingReason.OPENING_BALANCE, PostingReason.TRANSFER_DEBIT, PostingReason.COMPENSATION);
        assertThat(ledger.findByAccountIdOrderByCreatedAtAsc(destination)).isEmpty();
    }

    @Test
    void insufficientFundsFailsAfterTheCommandIsAccepted() throws Exception {
        UUID source = openAccount("Mary Jackson", "10.00");
        UUID destination = openAccount("Dorothy Vaughan", "0.00");

        UUID transferId = submit(source, destination, "50.00", UUID.randomUUID().toString());

        Transfer transfer = awaitStatus(transferId, TransferStatus.FAILED);
        assertThat(transfer.getFailureCode()).isEqualTo("INSUFFICIENT_FUNDS");
        assertThat(balance(source)).isEqualByComparingTo("10.00");
        assertThat(balance(destination)).isEqualByComparingTo("0.00");
    }

    @Test
    void idempotencyKeyReplaysTheOriginalTransfer() throws Exception {
        UUID source = openAccount("Annie Easley", "40.00");
        UUID destination = openAccount("Evelyn Boyd", "0.00");
        String key = UUID.randomUUID().toString();

        UUID first = submit(source, destination, "5.00", key);
        MvcResult replay = mockMvc.perform(post("/api/v1/transfers")
                        .header("Idempotency-Key", key)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(transferJson(source, destination, "5.00")))
                .andExpect(status().isOk())
                .andReturn();

        assertThat(UUID.fromString(JsonPath.read(replay.getResponse().getContentAsString(), "$.id"))).isEqualTo(first);
        mockMvc.perform(post("/api/v1/transfers")
                        .header("Idempotency-Key", key)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(transferJson(source, destination, "6.00")))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("IDEMPOTENCY_CONFLICT"));
    }

    @Test
    void structuralRejectionsDoNotOpenATransfer() throws Exception {
        UUID source = openAccount("Radia Perlman", "20.00");
        UUID eur = openAccount("EUR", "20.00", "EUR");

        mockMvc.perform(post("/api/v1/transfers")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(transferJson(source, eur, "1.00")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("MISSING_HEADER"));
        mockMvc.perform(post("/api/v1/transfers")
                        .header("Idempotency-Key", UUID.randomUUID().toString())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(transferJson(source, source, "1.00")))
                .andExpect(status().isUnprocessableContent())
                .andExpect(jsonPath("$.code").value("SAME_ACCOUNT"));
        mockMvc.perform(post("/api/v1/transfers")
                        .header("Idempotency-Key", UUID.randomUUID().toString())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(transferJson(source, eur, "1.00")))
                .andExpect(status().isUnprocessableContent())
                .andExpect(jsonPath("$.code").value("CURRENCY_MISMATCH"));

        mockMvc.perform(post("/api/v1/accounts/{id}/freeze", source)).andExpect(status().isOk());
        UUID other = openAccount("Frances Allen", "0.00");
        mockMvc.perform(post("/api/v1/transfers")
                        .header("Idempotency-Key", UUID.randomUUID().toString())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(transferJson(source, other, "1.00")))
                .andExpect(status().isUnprocessableContent())
                .andExpect(jsonPath("$.code").value("ACCOUNT_NOT_ACTIVE"));
    }

    @Test
    void concurrentDebitsCannotOverdrawTheSource() throws Exception {
        UUID source = openAccount("Shared Treasury", "100.00");
        UUID firstDestination = openAccount("Desk A", "0.00");
        UUID secondDestination = openAccount("Desk B", "0.00");
        CountDownLatch ready = new CountDownLatch(2);
        CountDownLatch go = new CountDownLatch(1);
        ExecutorService pool = Executors.newFixedThreadPool(2);
        try {
            Future<UUID> first = pool.submit(() -> submitTogether(source, firstDestination, ready, go));
            Future<UUID> second = pool.submit(() -> submitTogether(source, secondDestination, ready, go));
            assertThat(ready.await(10, SECONDS)).isTrue();
            go.countDown();
            UUID firstId = first.get(10, SECONDS);
            UUID secondId = second.get(10, SECONDS);

            await().atMost(Duration.ofSeconds(30)).pollInterval(Duration.ofMillis(200)).until(() ->
                    terminal(firstId) && terminal(secondId));

            TransferStatus firstStatus = transfers.findById(firstId).orElseThrow().getStatus();
            TransferStatus secondStatus = transfers.findById(secondId).orElseThrow().getStatus();
            assertThat(List.of(firstStatus, secondStatus))
                    .containsExactlyInAnyOrder(TransferStatus.COMPLETED, TransferStatus.FAILED);
            assertThat(balance(source)).isEqualByComparingTo("20.00");
            assertThat(balance(firstDestination).add(balance(secondDestination))).isEqualByComparingTo("80.00");
        } finally {
            pool.shutdownNow();
        }
    }

    @Test
    void ledgerEndpointReturnsOpeningAndTransferPostings() throws Exception {
        UUID source = openAccount("Ledger Reader", "15.00");
        UUID destination = openAccount("Ledger Dest", "0.00");
        submit(source, destination, "5.00", UUID.randomUUID().toString());
        await().atMost(Duration.ofSeconds(30)).until(() ->
                ledger.findByAccountIdOrderByCreatedAtAsc(source).size() == 2);

        mockMvc.perform(get("/api/v1/accounts/{id}/ledger", source))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].postingReason").value("OPENING_BALANCE"))
                .andExpect(jsonPath("$[1].postingReason").value("TRANSFER_DEBIT"))
                .andExpect(jsonPath("$[1].balanceAfter").value("10.00"));
    }

    private UUID submitTogether(UUID source, UUID destination, CountDownLatch ready, CountDownLatch go) throws Exception {
        ready.countDown();
        assertThat(go.await(10, SECONDS)).isTrue();
        return submit(source, destination, "80.00", UUID.randomUUID().toString());
    }

    private boolean terminal(UUID transferId) {
        TransferStatus status = transfers.findById(transferId).orElseThrow().getStatus();
        return status == TransferStatus.COMPLETED || status == TransferStatus.FAILED || status == TransferStatus.ROLLED_BACK;
    }

    private Transfer awaitStatus(UUID transferId, TransferStatus expected) {
        await().atMost(Duration.ofSeconds(30)).pollInterval(Duration.ofMillis(200)).until(() ->
                transfers.findById(transferId).orElseThrow().getStatus() == expected);
        return transfers.findById(transferId).orElseThrow();
    }

    private java.math.BigDecimal balance(UUID accountId) {
        return accounts.findById(accountId).orElseThrow().getAvailableBalance();
    }

    private UUID openAccount(String name, String balance) throws Exception {
        return openAccount(name, balance, "USD");
    }

    private UUID openAccount(String name, String balance, String currency) throws Exception {
        String body = mockMvc.perform(post("/api/v1/accounts")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"holderName":"%s","currency":"%s","openingBalance":"%s"}
                                """.formatted(name, currency, balance)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.availableBalance").value(balance))
                .andReturn()
                .getResponse()
                .getContentAsString();
        return UUID.fromString(JsonPath.read(body, "$.id"));
    }

    private UUID submit(UUID source, UUID destination, String amount, String key) throws Exception {
        String body = mockMvc.perform(post("/api/v1/transfers")
                        .header("Idempotency-Key", key)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(transferJson(source, destination, amount)))
                .andExpect(status().isAccepted())
                .andReturn()
                .getResponse()
                .getContentAsString();
        return UUID.fromString(JsonPath.read(body, "$.id"));
    }

    private static String transferJson(UUID source, UUID destination, String amount) {
        return """
                {"sourceAccountId":"%s","destinationAccountId":"%s","amount":"%s","currency":"USD"}
                """.formatted(source, destination, amount);
    }
}
