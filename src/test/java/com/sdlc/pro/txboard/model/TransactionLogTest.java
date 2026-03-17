package com.sdlc.pro.txboard.model;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.sdlc.pro.txboard.enums.IsolationLevel;
import com.sdlc.pro.txboard.enums.PropagationBehavior;
import com.sdlc.pro.txboard.enums.TransactionPhaseStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.Instant;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("TransactionLog Unit Tests")
class TransactionLogTest {

    private Instant startTime;
    private Instant endTime;
    private ConnectionSummary connectionSummary;
    private List<String> executedQueries;
    private List<TransactionEvent> events;
    private long txAlarmingThreshold;

    @BeforeEach
    void setUp() {
        startTime = Instant.parse("2023-01-01T10:00:00Z");
        endTime = Instant.parse("2023-01-01T10:00:01Z"); // 1 second duration
        connectionSummary = new ConnectionSummary(2, 1, 500L);
        executedQueries = Arrays.asList("SELECT * FROM users", "INSERT INTO orders");
        events = Arrays.asList(
                new TransactionEvent(TransactionEvent.Type.TRANSACTION_START, "Transaction started"),
                new TransactionEvent(TransactionEvent.Type.TRANSACTION_END, "Transaction completed")
        );
        txAlarmingThreshold = 1000L; // 1 second threshold
    }

    @Nested
    @DisplayName("Basic Construction Tests")
    class BasicConstructionTests {

        @Test
        @DisplayName("Should create TransactionLog with all fields correctly initialized")
        void shouldCreateTransactionLogWithAllFields() {
            // Given
            UUID txId = UUID.randomUUID();
            String method = "UserService.createUser";
            PropagationBehavior propagation = PropagationBehavior.REQUIRED;
            IsolationLevel isolation = IsolationLevel.READ_COMMITTED;
            TransactionPhaseStatus status = TransactionPhaseStatus.COMMITTED;
            String thread = "main-thread";

            // When
            TransactionLog transactionLog = new TransactionLog(
                    txId, method, propagation, isolation, startTime, endTime,
                    connectionSummary, status, thread, executedQueries, null, events, txAlarmingThreshold
            );

            // Then
            assertThat(transactionLog.getTxId()).isEqualTo(txId);
            assertThat(transactionLog.getMethod()).isEqualTo(method);
            assertThat(transactionLog.getPropagation()).isEqualTo(propagation);
            assertThat(transactionLog.getIsolation()).isEqualTo(isolation);
            assertThat(transactionLog.getStartTime()).isEqualTo(startTime);
            assertThat(transactionLog.getEndTime()).isEqualTo(endTime);
            assertThat(transactionLog.getStatus()).isEqualTo(status);
            assertThat(transactionLog.getThread()).isEqualTo(thread);
            assertThat(transactionLog.getConnectionSummary()).isEqualTo(connectionSummary);
            assertThat(transactionLog.getExecutedQuires()).isEqualTo(executedQueries);
            assertThat(transactionLog.getEvents()).isEqualTo(events);
        }

        @Test
        @DisplayName("Should calculate duration correctly from startTime and endTime")
        void shouldCalculateDurationCorrectly() {
            // Given
            Instant start = Instant.parse("2023-01-01T10:00:00Z");
            Instant end = Instant.parse("2023-01-01T10:00:02.500Z"); // 2.5 seconds

            // When
            TransactionLog transactionLog = new TransactionLog(
                    UUID.randomUUID(), "test", PropagationBehavior.REQUIRED, IsolationLevel.DEFAULT,
                    start, end, null, TransactionPhaseStatus.COMMITTED, "thread",
                    null, null, null, 1000L
            );

            // Then
            assertThat(transactionLog.getDuration()).isEqualTo(2500L);
        }

        @Test
        @DisplayName("Should handle zero duration correctly")
        void shouldHandleZeroDurationCorrectly() {
            // Given
            Instant sameTime = Instant.parse("2023-01-01T10:00:00Z");

            // When
            TransactionLog transactionLog = new TransactionLog(
                    UUID.randomUUID(), "test", PropagationBehavior.REQUIRED, IsolationLevel.DEFAULT,
                    sameTime, sameTime, null, TransactionPhaseStatus.COMMITTED, "thread",
                    null, null, null, 1000L
            );

            // Then
            assertThat(transactionLog.getDuration()).isEqualTo(0L);
        }
    }

    @Nested
    @DisplayName("Computed Fields Tests")
    class ComputedFieldsTests {

        @Test
        @DisplayName("connectionOriented should be true when connectionSummary.acquisitionCount() > 0")
        void connectionOrientedShouldBeTrueWhenAcquisitionCountGreaterThanZero() {
            // Given
            ConnectionSummary summary = new ConnectionSummary(2, 0, 100L);

            // When
            TransactionLog transactionLog = new TransactionLog(
                    UUID.randomUUID(), "test", PropagationBehavior.REQUIRED, IsolationLevel.DEFAULT,
                    startTime, endTime, summary, TransactionPhaseStatus.COMMITTED, "thread",
                    null, null, null, 1000L
            );

            // Then
            assertThat(transactionLog.getConnectionOriented()).isTrue();
        }

        @Test
        @DisplayName("connectionOriented should be false when connectionSummary.acquisitionCount() = 0")
        void connectionOrientedShouldBeFalseWhenAcquisitionCountIsZero() {
            // Given
            ConnectionSummary summary = new ConnectionSummary(0, 0, 0L);

            // When
            TransactionLog transactionLog = new TransactionLog(
                    UUID.randomUUID(), "test", PropagationBehavior.REQUIRED, IsolationLevel.DEFAULT,
                    startTime, endTime, summary, TransactionPhaseStatus.COMMITTED, "thread",
                    null, null, null, 1000L
            );

            // Then
            assertThat(transactionLog.getConnectionOriented()).isFalse();
        }

        @Test
        @DisplayName("connectionOriented should be null when connectionSummary is null")
        void connectionOrientedShouldBeNullWhenConnectionSummaryIsNull() {
            // When
            TransactionLog transactionLog = new TransactionLog(
                    UUID.randomUUID(), "test", PropagationBehavior.REQUIRED, IsolationLevel.DEFAULT,
                    startTime, endTime, null, TransactionPhaseStatus.COMMITTED, "thread",
                    null, null, null, 1000L
            );

            // Then
            assertThat(transactionLog.getConnectionOriented()).isNull();
        }

        @Test
        @DisplayName("alarmingTransaction should be true when duration exceeds threshold")
        void alarmingTransactionShouldBeTrueWhenDurationExceedsThreshold() {
            // Given
            Instant start = Instant.parse("2023-01-01T10:00:00Z");
            Instant end = Instant.parse("2023-01-01T10:00:02Z"); // 2 seconds
            long threshold = 1000L; // 1 second

            // When
            TransactionLog transactionLog = new TransactionLog(
                    UUID.randomUUID(), "test", PropagationBehavior.REQUIRED, IsolationLevel.DEFAULT,
                    start, end, null, TransactionPhaseStatus.COMMITTED, "thread",
                    null, null, null, threshold
            );

            // Then
            assertThat(transactionLog.isAlarmingTransaction()).isTrue();
        }

        @Test
        @DisplayName("alarmingTransaction should be false when duration is within threshold")
        void alarmingTransactionShouldBeFalseWhenDurationWithinThreshold() {
            // Given
            Instant start = Instant.parse("2023-01-01T10:00:00Z");
            Instant end = Instant.parse("2023-01-01T10:00:00.500Z"); // 500ms
            long threshold = 1000L; // 1 second

            // When
            TransactionLog transactionLog = new TransactionLog(
                    UUID.randomUUID(), "test", PropagationBehavior.REQUIRED, IsolationLevel.DEFAULT,
                    start, end, null, TransactionPhaseStatus.COMMITTED, "thread",
                    null, null, null, threshold
            );

            // Then
            assertThat(transactionLog.isAlarmingTransaction()).isFalse();
        }

        @Test
        @DisplayName("alarmingTransaction should be false when duration equals threshold")
        void alarmingTransactionShouldBeFalseWhenDurationEqualsThreshold() {
            // Given
            Instant start = Instant.parse("2023-01-01T10:00:00Z");
            Instant end = Instant.parse("2023-01-01T10:00:01Z"); // exactly 1 second
            long threshold = 1000L; // 1 second

            // When
            TransactionLog transactionLog = new TransactionLog(
                    UUID.randomUUID(), "test", PropagationBehavior.REQUIRED, IsolationLevel.DEFAULT,
                    start, end, null, TransactionPhaseStatus.COMMITTED, "thread",
                    null, null, null, threshold
            );

            // Then
            assertThat(transactionLog.isAlarmingTransaction()).isFalse();
        }

        @Test
        @DisplayName("havingAlarmingConnection should be true when connectionSummary.alarmingConnectionCount() > 0")
        void havingAlarmingConnectionShouldBeTrueWhenAlarmingConnectionCountGreaterThanZero() {
            // Given
            ConnectionSummary summary = new ConnectionSummary(1, 2, 100L);

            // When
            TransactionLog transactionLog = new TransactionLog(
                    UUID.randomUUID(), "test", PropagationBehavior.REQUIRED, IsolationLevel.DEFAULT,
                    startTime, endTime, summary, TransactionPhaseStatus.COMMITTED, "thread",
                    null, null, null, 1000L
            );

            // Then
            assertThat(transactionLog.getHavingAlarmingConnection()).isTrue();
        }

        @Test
        @DisplayName("havingAlarmingConnection should be false when connectionSummary.alarmingConnectionCount() = 0")
        void havingAlarmingConnectionShouldBeFalseWhenAlarmingConnectionCountIsZero() {
            // Given
            ConnectionSummary summary = new ConnectionSummary(1, 0, 100L);

            // When
            TransactionLog transactionLog = new TransactionLog(
                    UUID.randomUUID(), "test", PropagationBehavior.REQUIRED, IsolationLevel.DEFAULT,
                    startTime, endTime, summary, TransactionPhaseStatus.COMMITTED, "thread",
                    null, null, null, 1000L
            );

            // Then
            assertThat(transactionLog.getHavingAlarmingConnection()).isFalse();
        }

        @Test
        @DisplayName("havingAlarmingConnection should be null when connectionSummary is null")
        void havingAlarmingConnectionShouldBeNullWhenConnectionSummaryIsNull() {
            // When
            TransactionLog transactionLog = new TransactionLog(
                    UUID.randomUUID(), "test", PropagationBehavior.REQUIRED, IsolationLevel.DEFAULT,
                    startTime, endTime, null, TransactionPhaseStatus.COMMITTED, "thread",
                    null, null, null, 1000L
            );

            // Then
            assertThat(transactionLog.getHavingAlarmingConnection()).isNull();
        }
    }

    @Nested
    @DisplayName("Child Transactions Tests")
    class ChildTransactionsTests {

        @Test
        @DisplayName("getChild should return empty list when child is null")
        void getChildShouldReturnEmptyListWhenChildIsNull() {
            // When
            TransactionLog transactionLog = new TransactionLog(
                    UUID.randomUUID(), "test", PropagationBehavior.REQUIRED, IsolationLevel.DEFAULT,
                    startTime, endTime, null, TransactionPhaseStatus.COMMITTED, "thread",
                    null, null, null, 1000L
            );

            // Then
            assertThat(transactionLog.getChild()).isEmpty();
        }

        @Test
        @DisplayName("getChild should return provided child list when not null")
        void getChildShouldReturnProvidedChildListWhenNotNull() {
            // Given
            List<TransactionLog> children = Arrays.asList(
                    createChildTransactionLog(UUID.randomUUID()),
                    createChildTransactionLog(UUID.randomUUID())
            );

            // When
            TransactionLog transactionLog = new TransactionLog(
                    UUID.randomUUID(), "test", PropagationBehavior.REQUIRED, IsolationLevel.DEFAULT,
                    startTime, endTime, null, TransactionPhaseStatus.COMMITTED, "thread",
                    null, children, null, 1000L
            );

            // Then
            assertThat(transactionLog.getChild()).hasSize(2);
            assertThat(transactionLog.getChild()).containsExactlyElementsOf(children);
        }

        @Test
        @DisplayName("getTotalTransactionCount should return 1 for transaction without children")
        void getTotalTransactionCountShouldReturnOneForTransactionWithoutChildren() {
            // When
            TransactionLog transactionLog = new TransactionLog(
                    UUID.randomUUID(), "test", PropagationBehavior.REQUIRED, IsolationLevel.DEFAULT,
                    startTime, endTime, null, TransactionPhaseStatus.COMMITTED, "thread",
                    null, null, null, 1000L
            );

            // Then
            assertThat(transactionLog.getTotalTransactionCount()).isEqualTo(1);
        }

        @Test
        @DisplayName("getTotalTransactionCount should recursively count all child transactions")
        void getTotalTransactionCountShouldRecursivelyCountAllChildTransactions() {
            // Given
            TransactionLog grandChild1 = createChildTransactionLog(UUID.randomUUID());
            TransactionLog grandChild2 = createChildTransactionLog(UUID.randomUUID());
            TransactionLog child1 = new TransactionLog(
                    UUID.randomUUID(), "child1", PropagationBehavior.REQUIRED, IsolationLevel.DEFAULT,
                    startTime, endTime, null, TransactionPhaseStatus.COMMITTED, "thread",
                    null, Arrays.asList(grandChild1, grandChild2), null, 1000L
            );
            TransactionLog child2 = createChildTransactionLog(UUID.randomUUID());

            // When
            TransactionLog parent = new TransactionLog(
                    UUID.randomUUID(), "parent", PropagationBehavior.REQUIRED, IsolationLevel.DEFAULT,
                    startTime, endTime, null, TransactionPhaseStatus.COMMITTED, "thread",
                    null, Arrays.asList(child1, child2), null, 1000L
            );

            // Then
            // Parent(1) + Child1(1) + GrandChild1(1) + GrandChild2(1) + Child2(1) = 5
            assertThat(parent.getTotalTransactionCount()).isEqualTo(5);
        }

        @Test
        @DisplayName("getTotalQueryCount should return 0 for transaction without queries and children")
        void getTotalQueryCountShouldReturnZeroForTransactionWithoutQueriesAndChildren() {
            // When
            TransactionLog transactionLog = new TransactionLog(
                    UUID.randomUUID(), "test", PropagationBehavior.REQUIRED, IsolationLevel.DEFAULT,
                    startTime, endTime, null, TransactionPhaseStatus.COMMITTED, "thread",
                    null, null, null, 1000L
            );

            // Then
            assertThat(transactionLog.getTotalQueryCount()).isEqualTo(0);
        }

        @Test
        @DisplayName("getTotalQueryCount should recursively count all queries from children")
        void getTotalQueryCountShouldRecursivelyCountAllQueriesFromChildren() {
            // Given
            List<String> parentQueries = Arrays.asList("SELECT * FROM parent");
            List<String> childQueries = Arrays.asList("SELECT * FROM child1", "SELECT * FROM child2");
            List<String> grandChildQueries = Arrays.asList("SELECT * FROM grandchild");

            TransactionLog grandChild = new TransactionLog(
                    UUID.randomUUID(), "grandchild", PropagationBehavior.REQUIRED, IsolationLevel.DEFAULT,
                    startTime, endTime, null, TransactionPhaseStatus.COMMITTED, "thread",
                    grandChildQueries, null, null, 1000L
            );

            TransactionLog child = new TransactionLog(
                    UUID.randomUUID(), "child", PropagationBehavior.REQUIRED, IsolationLevel.DEFAULT,
                    startTime, endTime, null, TransactionPhaseStatus.COMMITTED, "thread",
                    childQueries, Arrays.asList(grandChild), null, 1000L
            );

            // When
            TransactionLog parent = new TransactionLog(
                    UUID.randomUUID(), "parent", PropagationBehavior.REQUIRED, IsolationLevel.DEFAULT,
                    startTime, endTime, null, TransactionPhaseStatus.COMMITTED, "thread",
                    parentQueries, Arrays.asList(child), null, 1000L
            );

            // Then
            // Parent(1) + Child(2) + GrandChild(1) = 4
            assertThat(parent.getTotalQueryCount()).isEqualTo(4);
        }
    }

    @Nested
    @DisplayName("Executed Queries Tests")
    class ExecutedQueriesTests {

        @Test
        @DisplayName("getExecutedQuires should return empty list when executedQuires is null")
        void getExecutedQuiresShouldReturnEmptyListWhenExecutedQuiresIsNull() {
            // When
            TransactionLog transactionLog = new TransactionLog(
                    UUID.randomUUID(), "test", PropagationBehavior.REQUIRED, IsolationLevel.DEFAULT,
                    startTime, endTime, null, TransactionPhaseStatus.COMMITTED, "thread",
                    null, null, null, 1000L
            );

            // Then
            assertThat(transactionLog.getExecutedQuires()).isEmpty();
        }

        @Test
        @DisplayName("getExecutedQuires should return provided queries when not null")
        void getExecutedQuiresShouldReturnProvidedQueriesWhenNotNull() {
            // Given
            List<String> queries = Arrays.asList("SELECT * FROM users", "INSERT INTO orders");

            // When
            TransactionLog transactionLog = new TransactionLog(
                    UUID.randomUUID(), "test", PropagationBehavior.REQUIRED, IsolationLevel.DEFAULT,
                    startTime, endTime, null, TransactionPhaseStatus.COMMITTED, "thread",
                    queries, null, null, 1000L
            );

            // Then
            assertThat(transactionLog.getExecutedQuires()).hasSize(2);
            assertThat(transactionLog.getExecutedQuires()).containsExactlyElementsOf(queries);
        }
    }

    @Nested
    @DisplayName("Health Checks Tests")
    class HealthChecksTests {

        @Test
        @DisplayName("isHealthyTransaction should return true when no alarming transaction and no alarming connection")
        void isHealthyTransactionShouldReturnTrueWhenNoAlarmingTransactionAndNoAlarmingConnection() {
            // Given
            ConnectionSummary summary = new ConnectionSummary(1, 0, 100L);
            Instant start = Instant.parse("2023-01-01T10:00:00Z");
            Instant end = Instant.parse("2023-01-01T10:00:00.500Z"); // 500ms, within threshold

            // When
            TransactionLog transactionLog = new TransactionLog(
                    UUID.randomUUID(), "test", PropagationBehavior.REQUIRED, IsolationLevel.DEFAULT,
                    start, end, summary, TransactionPhaseStatus.COMMITTED, "thread",
                    null, null, null, 1000L
            );

            // Then
            assertThat(transactionLog.isHealthyTransaction()).isTrue();
        }

        @Test
        @DisplayName("isHealthyTransaction should return false when transaction is alarming")
        void isHealthyTransactionShouldReturnFalseWhenTransactionIsAlarming() {
            // Given
            ConnectionSummary summary = new ConnectionSummary(1, 0, 100L);
            Instant start = Instant.parse("2023-01-01T10:00:00Z");
            Instant end = Instant.parse("2023-01-01T10:00:02Z"); // 2 seconds, exceeds threshold

            // When
            TransactionLog transactionLog = new TransactionLog(
                    UUID.randomUUID(), "test", PropagationBehavior.REQUIRED, IsolationLevel.DEFAULT,
                    start, end, summary, TransactionPhaseStatus.COMMITTED, "thread",
                    null, null, null, 1000L
            );

            // Then
            assertThat(transactionLog.isHealthyTransaction()).isFalse();
        }

        @Test
        @DisplayName("isHealthyTransaction should return false when having alarming connection")
        void isHealthyTransactionShouldReturnFalseWhenHavingAlarmingConnection() {
            // Given
            ConnectionSummary summary = new ConnectionSummary(1, 1, 100L);
            Instant start = Instant.parse("2023-01-01T10:00:00Z");
            Instant end = Instant.parse("2023-01-01T10:00:00.500Z"); // 500ms, within threshold

            // When
            TransactionLog transactionLog = new TransactionLog(
                    UUID.randomUUID(), "test", PropagationBehavior.REQUIRED, IsolationLevel.DEFAULT,
                    start, end, summary, TransactionPhaseStatus.COMMITTED, "thread",
                    null, null, null, 1000L
            );

            // Then
            assertThat(transactionLog.isHealthyTransaction()).isFalse();
        }

        @Test
        @DisplayName("isHealthyTransaction should return false when both transaction and connection are alarming")
        void isHealthyTransactionShouldReturnFalseWhenBothTransactionAndConnectionAreAlarming() {
            // Given
            ConnectionSummary summary = new ConnectionSummary(1, 1, 100L);
            Instant start = Instant.parse("2023-01-01T10:00:00Z");
            Instant end = Instant.parse("2023-01-01T10:00:02Z"); // 2 seconds, exceeds threshold

            // When
            TransactionLog transactionLog = new TransactionLog(
                    UUID.randomUUID(), "test", PropagationBehavior.REQUIRED, IsolationLevel.DEFAULT,
                    start, end, summary, TransactionPhaseStatus.COMMITTED, "thread",
                    null, null, null, 1000L
            );

            // Then
            assertThat(transactionLog.isHealthyTransaction()).isFalse();
        }

        @Test
        @DisplayName("isHealthyTransaction should return true when connectionSummary is null and transaction is not alarming")
        void isHealthyTransactionShouldReturnTrueWhenConnectionSummaryIsNullAndTransactionNotAlarming() {
            // Given
            Instant start = Instant.parse("2023-01-01T10:00:00Z");
            Instant end = Instant.parse("2023-01-01T10:00:00.500Z"); // 500ms, within threshold

            // When
            TransactionLog transactionLog = new TransactionLog(
                    UUID.randomUUID(), "test", PropagationBehavior.REQUIRED, IsolationLevel.DEFAULT,
                    start, end, null, TransactionPhaseStatus.COMMITTED, "thread",
                    null, null, null, 1000L
            );

            // Then
            assertThat(transactionLog.isHealthyTransaction()).isTrue();
        }

        @Test
        @DisplayName("isHealthyTransaction should return false when connectionSummary is null but transaction is alarming")
        void isHealthyTransactionShouldReturnFalseWhenConnectionSummaryIsNullButTransactionIsAlarming() {
            // Given
            Instant start = Instant.parse("2023-01-01T10:00:00Z");
            Instant end = Instant.parse("2023-01-01T10:00:02Z"); // 2 seconds, exceeds threshold

            // When
            TransactionLog transactionLog = new TransactionLog(
                    UUID.randomUUID(), "test", PropagationBehavior.REQUIRED, IsolationLevel.DEFAULT,
                    start, end, null, TransactionPhaseStatus.COMMITTED, "thread",
                    null, null, null, 1000L
            );

            // Then
            assertThat(transactionLog.isHealthyTransaction()).isFalse();
        }
    }

    @Nested
    @DisplayName("Events Tests")
    class EventsTests {

        @Test
        @DisplayName("getEvents should return the provided events list")
        void getEventsShouldReturnTheProvidedEventsList() {
            // Given
            List<TransactionEvent> events = Arrays.asList(
                    new TransactionEvent(TransactionEvent.Type.TRANSACTION_START, "Started"),
                    new TransactionEvent(TransactionEvent.Type.TRANSACTION_END, "Completed")
            );

            // When
            TransactionLog transactionLog = new TransactionLog(
                    UUID.randomUUID(), "test", PropagationBehavior.REQUIRED, IsolationLevel.DEFAULT,
                    startTime, endTime, null, TransactionPhaseStatus.COMMITTED, "thread",
                    null, null, events, 1000L
            );

            // Then
            assertThat(transactionLog.getEvents()).isEqualTo(events);
        }

        @Test
        @DisplayName("getEvents should return null when events is null")
        void getEventsShouldReturnNullWhenEventsIsNull() {
            // When
            TransactionLog transactionLog = new TransactionLog(
                    UUID.randomUUID(), "test", PropagationBehavior.REQUIRED, IsolationLevel.DEFAULT,
                    startTime, endTime, null, TransactionPhaseStatus.COMMITTED, "thread",
                    null, null, null, 1000L
            );

            // Then
            assertThat(transactionLog.getEvents()).isNull();
        }
    }

    @Nested
    @DisplayName("Serialization Tests")
    class SerializationTests {

        @Test
        @DisplayName("Should exclude null fields in JSON serialization due to @JsonInclude(NON_NULL)")
        void shouldExcludeNullFieldsInJsonSerialization() throws Exception {
            // Given
            TransactionLog transactionLog = new TransactionLog(
                    UUID.randomUUID(), "test", PropagationBehavior.REQUIRED, IsolationLevel.DEFAULT,
                    startTime, endTime, null, TransactionPhaseStatus.COMMITTED, "thread",
                    null, null, null, 1000L
            );

            ObjectMapper objectMapper = new ObjectMapper();
            objectMapper.registerModule(new JavaTimeModule());

            // When
            String json = objectMapper.writeValueAsString(transactionLog);

            // Then
            assertThat(json).doesNotContain("connectionSummary");
            assertThat(json).doesNotContain("connectionOriented");
            assertThat(json).doesNotContain("events");
            assertThat(json).doesNotContain("havingAlarmingConnection");
            // Note: executedQuires and child are included as empty arrays because getters return List.of() instead of null
            assertThat(json).contains("executedQuires");
            assertThat(json).contains("child");
            assertThat(json).contains("txId");
            assertThat(json).contains("method");
            assertThat(json).contains("duration");
        }

        @Test
        @DisplayName("Should include non-null fields in JSON serialization")
        void shouldIncludeNonNullFieldsInJsonSerialization() throws Exception {
            // Given
            TransactionLog transactionLog = new TransactionLog(
                    UUID.randomUUID(), "test", PropagationBehavior.REQUIRED, IsolationLevel.DEFAULT,
                    startTime, endTime, connectionSummary, TransactionPhaseStatus.COMMITTED, "thread",
                    executedQueries, null, events, 1000L
            );

            ObjectMapper objectMapper = new ObjectMapper();
            objectMapper.registerModule(new JavaTimeModule());

            // When
            String json = objectMapper.writeValueAsString(transactionLog);

            // Then
            assertThat(json).contains("txId");
            assertThat(json).contains("method");
            assertThat(json).contains("duration");
            assertThat(json).contains("connectionSummary");
            assertThat(json).contains("connectionOriented");
            assertThat(json).contains("executedQuires");
            assertThat(json).contains("events");
        }
    }

    @Nested
    @DisplayName("Edge Cases Tests")
    class EdgeCasesTests {

        @Test
        @DisplayName("Should handle negative duration correctly")
        void shouldHandleNegativeDurationCorrectly() {
            // Given - endTime before startTime
            Instant start = Instant.parse("2023-01-01T10:00:01Z");
            Instant end = Instant.parse("2023-01-01T10:00:00Z");

            // When
            TransactionLog transactionLog = new TransactionLog(
                    UUID.randomUUID(), "test", PropagationBehavior.REQUIRED, IsolationLevel.DEFAULT,
                    start, end, null, TransactionPhaseStatus.COMMITTED, "thread",
                    null, null, null, 1000L
            );

            // Then
            assertThat(transactionLog.getDuration()).isEqualTo(-1000L);
        }

        @Test
        @DisplayName("Should handle very large duration correctly")
        void shouldHandleVeryLargeDurationCorrectly() {
            // Given
            Instant start = Instant.parse("2023-01-01T10:00:00Z");
            Instant end = Instant.parse("2023-01-01T10:00:00Z").plus(Duration.ofDays(1)); // 1 day

            // When
            TransactionLog transactionLog = new TransactionLog(
                    UUID.randomUUID(), "test", PropagationBehavior.REQUIRED, IsolationLevel.DEFAULT,
                    start, end, null, TransactionPhaseStatus.COMMITTED, "thread",
                    null, null, null, 1000L
            );

            // Then
            assertThat(transactionLog.getDuration()).isEqualTo(86400000L); // 1 day in milliseconds
            assertThat(transactionLog.isAlarmingTransaction()).isTrue();
        }

        @Test
        @DisplayName("Should handle zero threshold correctly")
        void shouldHandleZeroThresholdCorrectly() {
            // Given
            Instant start = Instant.parse("2023-01-01T10:00:00Z");
            Instant end = Instant.parse("2023-01-01T10:00:00.001Z"); // 1ms
            long threshold = 0L;

            // When
            TransactionLog transactionLog = new TransactionLog(
                    UUID.randomUUID(), "test", PropagationBehavior.REQUIRED, IsolationLevel.DEFAULT,
                    start, end, null, TransactionPhaseStatus.COMMITTED, "thread",
                    null, null, null, threshold
            );

            // Then
            assertThat(transactionLog.isAlarmingTransaction()).isTrue(); // Any duration > 0 exceeds threshold of 0
        }

        @Test
        @DisplayName("Should handle empty lists correctly")
        void shouldHandleEmptyListsCorrectly() {
            // Given
            List<String> emptyQueries = Arrays.asList();
            List<TransactionLog> emptyChildren = Arrays.asList();
            List<TransactionEvent> emptyEvents = Arrays.asList();

            // When
            TransactionLog transactionLog = new TransactionLog(
                    UUID.randomUUID(), "test", PropagationBehavior.REQUIRED, IsolationLevel.DEFAULT,
                    startTime, endTime, null, TransactionPhaseStatus.COMMITTED, "thread",
                    emptyQueries, emptyChildren, emptyEvents, 1000L
            );

            // Then
            assertThat(transactionLog.getExecutedQuires()).isEmpty();
            assertThat(transactionLog.getChild()).isEmpty();
            assertThat(transactionLog.getEvents()).isEmpty();
            assertThat(transactionLog.getTotalQueryCount()).isEqualTo(0);
            assertThat(transactionLog.getTotalTransactionCount()).isEqualTo(1);
        }
    }

    // Helper method to create child transaction logs
    private TransactionLog createChildTransactionLog(UUID txId) {
        return new TransactionLog(
                txId, "child" + txId, PropagationBehavior.REQUIRED, IsolationLevel.DEFAULT,
                startTime, endTime, null, TransactionPhaseStatus.COMMITTED, "thread",
                null, null, null, 1000L
        );
    }
}
