package com.sdlc.pro.txboard.listener;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.core.read.ListAppender;
import com.sdlc.pro.txboard.config.TxBoardProperties;
import com.sdlc.pro.txboard.enums.TransactionPhaseStatus;
import com.sdlc.pro.txboard.model.TransactionLog;
import org.junit.jupiter.api.*;
import org.mockito.ArgumentCaptor;
import org.slf4j.LoggerFactory;
import org.springframework.transaction.TransactionDefinition;

import java.util.Arrays;
import java.util.List;
import java.util.concurrent.*;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class TransactionPhaseListenerImplTest {

    private TxBoardProperties txBoardProperties;
    private TransactionDefinition txDefinition;
    private TransactionLogListener txLogListener;
    private TransactionPhaseListenerImpl txListener;
    private Logger logger;
    private ListAppender<ch.qos.logback.classic.spi.ILoggingEvent> appender;

    private void sleep(long ms) {
        try { Thread.sleep(ms); } catch (InterruptedException e) { Thread.currentThread().interrupt(); }
    }

    @BeforeEach
    void setup() {
        txBoardProperties = new TxBoardProperties();
        txDefinition = mock(TransactionDefinition.class);
        txLogListener = mock(TransactionLogListener.class);
        txListener = new TransactionPhaseListenerImpl(Arrays.asList(txLogListener), txBoardProperties);

        logger = (Logger) LoggerFactory.getLogger(TransactionPhaseListenerImpl.class);
        appender = new ListAppender<>();
        appender.start();
        logger.addAppender(appender);
    }

    @AfterEach
    void tearDown() {
        logger.detachAppender(appender);
    }

    @Nested
    class TransactionLifecycleTests {

        @Test
        void shouldCommitTransaction() {
            when(txDefinition.getName()).thenReturn("UserService.createUser");

            txListener.beforeBegin(txDefinition);
            txListener.afterBegin(null);
            txListener.afterAcquiredConnection();
            txListener.executedQuery("INSERT INTO users VALUES (?,?)");
            sleep(5);
            txListener.afterCommit();
            txListener.afterCloseConnection();

            ArgumentCaptor<TransactionLog> captor = ArgumentCaptor.forClass(TransactionLog.class);
            verify(txLogListener).listen(captor.capture());

            TransactionLog log = captor.getValue();
            assertEquals(TransactionPhaseStatus.COMMITTED, log.getStatus());
            assertEquals(1, log.getTotalQueryCount());
            assertEquals("INSERT INTO users VALUES (?,?)", log.getExecutedQuires().get(0));
        }

        @Test
        void shouldRollbackTransaction() {
            when(txDefinition.getName()).thenReturn("OrderService.process");

            txListener.beforeBegin(txDefinition);
            txListener.afterBegin(null);
            txListener.afterAcquiredConnection();
            txListener.afterRollback();
            txListener.afterCloseConnection();

            ArgumentCaptor<TransactionLog> captor = ArgumentCaptor.forClass(TransactionLog.class);
            verify(txLogListener).listen(captor.capture());
            assertEquals(TransactionPhaseStatus.ROLLED_BACK, captor.getValue().getStatus());
        }

        @Test
        void shouldHandleErrorDuringTransaction() {
            txListener.beforeBegin(txDefinition);
            txListener.afterBegin(null);
            txListener.afterAcquiredConnection();
            txListener.errorOccurredAtTransactionPhase(new IllegalStateException("Error"));
            txListener.afterCloseConnection();

            ArgumentCaptor<TransactionLog> captor = ArgumentCaptor.forClass(TransactionLog.class);
            verify(txLogListener).listen(captor.capture());
            assertEquals(TransactionPhaseStatus.ERRORED, captor.getValue().getStatus());
        }
    }

    @Nested
    class ConnectionTrackingTests {

        @Test
        void shouldTrackConnectionsAndFinishAfterClose() {
            txListener.beforeBegin(txDefinition);
            txListener.afterBegin(null);
            txListener.afterAcquiredConnection();
            txListener.afterCommit();
            txListener.afterCloseConnection();
            verify(txLogListener, times(1)).listen(any());
        }

        @Test
        void shouldCalculateConnectionSummary() {
            txBoardProperties.getAlarmingThreshold().setConnection(10L);

            txListener.beforeBegin(txDefinition);
            txListener.afterBegin(null);

            txListener.afterAcquiredConnection();
            sleep(5);
            txListener.afterCloseConnection();

            txListener.afterAcquiredConnection();
            sleep(5);
            txListener.afterCloseConnection();

            txListener.afterCommit();
            txListener.afterCloseConnection();

            ArgumentCaptor<TransactionLog> captor = ArgumentCaptor.forClass(TransactionLog.class);
            verify(txLogListener).listen(captor.capture());
            assertEquals(TransactionPhaseStatus.COMMITTED, captor.getValue().getStatus());
        }

        @Test
        void shouldHandleMultipleConnectionsInSingleTransaction() {
            txListener.beforeBegin(txDefinition);
            txListener.afterBegin(null);

            for (int i = 0; i < 3; i++) {
                txListener.afterAcquiredConnection();
                txListener.executedQuery("QUERY_" + i); // track query per connection
                sleep(2);
                txListener.afterCloseConnection();
            }

            txListener.afterCommit();
            txListener.afterCloseConnection();

            ArgumentCaptor<TransactionLog> captor = ArgumentCaptor.forClass(TransactionLog.class);
            verify(txLogListener).listen(captor.capture());

            // Count queries as a proxy for connections
            assertEquals(3, captor.getValue().getExecutedQuires().size());
        }

    }

    @Nested
    class ListenerFailureAndLoggingTests {

        @Test
        void shouldLogErrorIfListenerFails() {
            doThrow(new RuntimeException("Listener failed")).when(txLogListener).listen(any());

            txListener.beforeBegin(txDefinition);
            txListener.afterBegin(null);
            txListener.afterAcquiredConnection();
            txListener.afterCommit();
            txListener.afterCloseConnection();

            List<ch.qos.logback.classic.spi.ILoggingEvent> errorLogs = appender.list.stream()
                    .filter(e -> e.getLevel() == Level.ERROR)
                    .collect(Collectors.toList());

            assertTrue(errorLogs.stream().anyMatch(e -> e.getFormattedMessage().contains("Failed to publish transaction log")));
        }

        @Test
        void shouldContinueTransactionEvenIfListenerFails() {
            doThrow(new RuntimeException("Listener failed")).when(txLogListener).listen(any());

            txListener.beforeBegin(txDefinition);
            txListener.afterBegin(null);
            txListener.afterAcquiredConnection();
            txListener.afterCommit();
            txListener.afterCloseConnection();
        }

        @Test
        void shouldLogErrorDuringRollbackIfListenerFails() {
            doThrow(new RuntimeException("Listener failed")).when(txLogListener).listen(any());

            txListener.beforeBegin(txDefinition);
            txListener.afterBegin(null);
            txListener.afterAcquiredConnection();
            txListener.afterRollback();
            txListener.afterCloseConnection();

            List<ch.qos.logback.classic.spi.ILoggingEvent> errors = appender.list.stream()
                    .filter(e -> e.getLevel() == Level.ERROR)
                    .collect(Collectors.toList());
            assertFalse(errors.isEmpty());
        }
    }

    @Nested
    class ConcurrencyTests {

        @Test
        void shouldHandleConcurrentTransactions() throws InterruptedException {
            ExecutorService executor = Executors.newFixedThreadPool(2);
            Callable<Void> task = () -> {
                txListener.beforeBegin(txDefinition);
                txListener.afterBegin(null);
                txListener.afterAcquiredConnection();
                sleep(5);
                txListener.afterCommit();
                txListener.afterCloseConnection();
                return null;
            };
            executor.invokeAll(Arrays.asList(task, task));
            executor.shutdownNow();
            verify(txLogListener, atLeast(2)).listen(any());
        }

        @Test
        void shouldHandleConcurrentErrors() throws InterruptedException {
            ExecutorService executor = Executors.newFixedThreadPool(2);
            Callable<Void> task = () -> {
                txListener.beforeBegin(txDefinition);
                txListener.afterBegin(null);
                txListener.afterAcquiredConnection();
                txListener.errorOccurredAtTransactionPhase(new RuntimeException("Error"));
                txListener.afterCloseConnection();
                return null;
            };
            executor.invokeAll(Arrays.asList(task, task));
            executor.shutdownNow();
            verify(txLogListener, atLeast(2)).listen(any());
        }

        @Test
        void shouldHandleConcurrentRollbacks() throws InterruptedException {
            ExecutorService executor = Executors.newFixedThreadPool(2);
            Callable<Void> task = () -> {
                txListener.beforeBegin(txDefinition);
                txListener.afterBegin(null);
                txListener.afterAcquiredConnection();
                txListener.afterRollback();
                txListener.afterCloseConnection();
                return null;
            };
            executor.invokeAll(Arrays.asList(task, task));
            executor.shutdownNow();
            verify(txLogListener, atLeast(2)).listen(any());
        }
    }

    @Nested
    class NestedTransactionTests {

        @Test
        void shouldTrackNestedTransactionsCorrectly() {
            when(txDefinition.getName()).thenReturn("ParentService.parentMethod");

            txListener.beforeBegin(txDefinition);
            txListener.afterBegin(null);
            txListener.afterAcquiredConnection();
            txListener.executedQuery("SELECT * FROM parent_table");

            TransactionDefinition childTx = mock(TransactionDefinition.class);
            when(childTx.getName()).thenReturn("ChildService.childMethod");

            txListener.beforeBegin(childTx);
            txListener.afterBegin(null);
            txListener.afterAcquiredConnection();
            txListener.executedQuery("CHILD_QUERY");
            txListener.afterCommit();
            txListener.afterCloseConnection();

            txListener.afterCommit();
            txListener.afterCloseConnection();

            ArgumentCaptor<TransactionLog> captor = ArgumentCaptor.forClass(TransactionLog.class);
            verify(txLogListener).listen(captor.capture());

            TransactionLog parentLog = captor.getValue();
            assertEquals(TransactionPhaseStatus.COMMITTED, parentLog.getStatus());
            assertEquals(2, parentLog.getTotalQueryCount());
        }

        @Test
        void shouldHandleNestedTransactionErrorsWithoutBreakingParent() {
            when(txDefinition.getName()).thenReturn("ParentService.parentMethod");

            txListener.beforeBegin(txDefinition);
            txListener.afterBegin(null);
            txListener.afterAcquiredConnection();

            TransactionDefinition childTx = mock(TransactionDefinition.class);
            when(childTx.getName()).thenReturn("ChildService.childMethod");

            txListener.beforeBegin(childTx);
            txListener.afterBegin(null);
            txListener.afterAcquiredConnection();
            txListener.errorOccurredAtTransactionPhase(new RuntimeException("Child error"));
            txListener.afterCloseConnection();

            txListener.afterCommit();
            txListener.afterCloseConnection();

            ArgumentCaptor<TransactionLog> captor = ArgumentCaptor.forClass(TransactionLog.class);
            verify(txLogListener).listen(captor.capture());

            TransactionLog childLog = captor.getValue().getChild().get(0);
            assertEquals(TransactionPhaseStatus.ERRORED, childLog.getStatus());
        }

        @Test
        void shouldHandleNestedCommitAndRollback() {
            when(txDefinition.getName()).thenReturn("ParentService.parentMethod");

            txListener.beforeBegin(txDefinition);
            txListener.afterBegin(null);
            txListener.afterAcquiredConnection();

            TransactionDefinition childTx = mock(TransactionDefinition.class);
            when(childTx.getName()).thenReturn("ChildService.childMethod");

            txListener.beforeBegin(childTx);
            txListener.afterBegin(null);
            txListener.afterAcquiredConnection();
            txListener.afterRollback();
            txListener.afterCloseConnection();

            txListener.afterCommit();
            txListener.afterCloseConnection();

            ArgumentCaptor<TransactionLog> captor = ArgumentCaptor.forClass(TransactionLog.class);
            verify(txLogListener).listen(captor.capture());

            TransactionLog childLog = captor.getValue().getChild().get(0);
            assertEquals(TransactionPhaseStatus.ROLLED_BACK, childLog.getStatus());
        }
    }
}
