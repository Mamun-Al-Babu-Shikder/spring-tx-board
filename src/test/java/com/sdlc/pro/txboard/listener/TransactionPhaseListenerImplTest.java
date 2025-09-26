package com.sdlc.pro.txboard.listener;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import com.sdlc.pro.txboard.config.TxBoardProperties;
import com.sdlc.pro.txboard.enums.IsolationLevel;
import com.sdlc.pro.txboard.enums.PropagationBehavior;
import com.sdlc.pro.txboard.enums.TransactionPhaseStatus;
import com.sdlc.pro.txboard.model.ConnectionSummary;
import com.sdlc.pro.txboard.model.TransactionEvent;
import com.sdlc.pro.txboard.model.TransactionLog;
import com.sdlc.pro.txboard.util.Utils;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.mockito.ArgumentCaptor;
import org.slf4j.LoggerFactory;
import org.springframework.transaction.TransactionDefinition;

import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.Stack;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Function;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class TransactionPhaseListenerImplTest {
    private TxBoardProperties txBoardProperties;
    private TransactionDefinition transactionDefinition;
    private TransactionLogListener txLogListener;
    private TransactionPhaseListenerImpl txPhaseListener;
    private ListAppender<ILoggingEvent> loggingEventAppender;
    private Logger logger;

    @BeforeEach
    void setup() {
        txBoardProperties = new TxBoardProperties();
        transactionDefinition = mock(TransactionDefinition.class);

        txLogListener = mock(TransactionLogListener.class);
        txPhaseListener = new TransactionPhaseListenerImpl(List.of(txLogListener), txBoardProperties);

        logger = (Logger) LoggerFactory.getLogger(TransactionPhaseListenerImpl.class);
        loggingEventAppender = new ListAppender<>();
        loggingEventAppender.start();
        logger.addAppender(loggingEventAppender);
    }

    @AfterEach
    void cleanUp() {
        logger.detachAppender(loggingEventAppender);
    }

    @Nested
    class TransactionLifecycleTests {

        @Test
        void shouldTrackCompleteTransactionLifecycleWithCommit() {
            when(transactionDefinition.getName()).thenReturn("com.example.service.UserService.createUser");
            when(transactionDefinition.getIsolationLevel()).thenReturn(TransactionDefinition.ISOLATION_DEFAULT);
            when(transactionDefinition.getPropagationBehavior()).thenReturn(TransactionDefinition.PROPAGATION_REQUIRED);

            txPhaseListener.beforeBegin(transactionDefinition);
            txPhaseListener.afterBegin(null);
            txPhaseListener.afterAcquiredConnection();
            txPhaseListener.executedQuery("INSERT INTO users VALUES (?, ?)");
            Utils.sleep(3);
            txPhaseListener.afterCloseConnection();
            txPhaseListener.afterCommit();

            ArgumentCaptor<TransactionLog> txLogCaptor = ArgumentCaptor.forClass(TransactionLog.class);
            verify(txLogListener).listen(txLogCaptor.capture());

            TransactionLog transactionLog = txLogCaptor.getValue();
            assertNotNull(transactionLog.getTxId());
            assertEquals("UserService.createUser", transactionLog.getMethod());
            assertEquals(IsolationLevel.DEFAULT, transactionLog.getIsolation());
            assertEquals(PropagationBehavior.REQUIRED, transactionLog.getPropagation());
            assertEquals(TransactionPhaseStatus.COMMITTED, transactionLog.getStatus());
            assertEquals(Thread.currentThread().getName(), transactionLog.getThread());
            assertTrue(transactionLog.getChild().isEmpty());
            assertTrue(transactionLog.getConnectionOriented());
            assertEquals(1, transactionLog.getTotalTransactionCount());
            assertEquals(1, transactionLog.getTotalQueryCount());
            assertEquals(1, transactionLog.getExecutedQuires().size());
            assertTrue(transactionLog.getExecutedQuires().contains("INSERT INTO users VALUES (?, ?)"));
            assertNotNull(transactionLog.getConnectionSummary());
            assertEquals(1, transactionLog.getConnectionSummary().acquisitionCount());
            assertNotNull(transactionLog.getStatus());
            assertNotNull(transactionLog.getEndTime());
            assertTrue(transactionLog.getDuration() >= 3);

            List<TransactionEvent> transactionEvents = transactionLog.getEvents();
            assertNotNull(transactionEvents);
            assertEquals(4, transactionEvents.size());
            assertEquals(TransactionEvent.Type.TRANSACTION_START, transactionEvents.get(0).getType());
            assertNotNull(transactionEvents.get(0).getTimestamp());

            assertEquals(TransactionEvent.Type.CONNECTION_ACQUIRED, transactionEvents.get(1).getType());
            assertNotNull(transactionEvents.get(1).getTimestamp());

            assertEquals(TransactionEvent.Type.CONNECTION_RELEASED, transactionEvents.get(2).getType());
            assertNotNull(transactionEvents.get(2).getTimestamp());

            assertEquals(TransactionEvent.Type.TRANSACTION_END, transactionEvents.get(3).getType());
            assertNotNull(transactionEvents.get(3).getTimestamp());
        }

        @Test
        void shouldTrackTransactionRollback() {
            txPhaseListener.beforeBegin(transactionDefinition);
            txPhaseListener.afterBegin(null);
            txPhaseListener.afterAcquiredConnection();
            txPhaseListener.executedQuery("UPDATE users SET name = ?");
            txPhaseListener.afterCloseConnection();
            txPhaseListener.afterRollback();

            ArgumentCaptor<TransactionLog> txLogCaptor = ArgumentCaptor.forClass(TransactionLog.class);
            verify(txLogListener).listen(txLogCaptor.capture());

            TransactionLog transactionLog = txLogCaptor.getValue();
            assertEquals(TransactionPhaseStatus.ROLLED_BACK, transactionLog.getStatus());
        }

        @Test
        void shouldHandleErrorDuringTransactionBegin() {
            RuntimeException exception = new RuntimeException("Failed to start transaction!");

            txPhaseListener.beforeBegin(transactionDefinition);
            txPhaseListener.afterBegin(exception);

            ArgumentCaptor<TransactionLog> txLogCaptor = ArgumentCaptor.forClass(TransactionLog.class);
            verify(txLogListener).listen(txLogCaptor.capture());

            TransactionLog transactionLog = txLogCaptor.getValue();
            assertEquals(TransactionPhaseStatus.ERRORED, transactionLog.getStatus());
        }

        @Test
        void shouldHandleErrorDuringTransactionPhase() {
            RuntimeException exception = new RuntimeException("Failed the transaction!");

            txPhaseListener.beforeBegin(transactionDefinition);
            txPhaseListener.afterBegin(null);
            txPhaseListener.errorOccurredAtTransactionPhase(exception);

            ArgumentCaptor<TransactionLog> txLogCaptor = ArgumentCaptor.forClass(TransactionLog.class);
            verify(txLogListener).listen(txLogCaptor.capture());

            TransactionLog transactionLog = txLogCaptor.getValue();
            assertEquals(TransactionPhaseStatus.ERRORED, transactionLog.getStatus());
        }

        @Test
        void shouldTrackAlarmingTransaction() {
            txBoardProperties.getAlarmingThreshold().setTransaction(10);
            txPhaseListener.beforeBegin(transactionDefinition);
            txPhaseListener.afterBegin(null);
            Utils.sleep(20);
            txPhaseListener.afterCommit();

            ArgumentCaptor<TransactionLog> txLogCaptor = ArgumentCaptor.forClass(TransactionLog.class);
            verify(txLogListener).listen(txLogCaptor.capture());

            TransactionLog transactionLog = txLogCaptor.getValue();
            assertTrue(transactionLog.isAlarmingTransaction());
        }
    }

    @Nested
    class NestedTransactionTests {

        @Test
        void shouldTrackNestedTransaction() {
            when(transactionDefinition.getName()).thenReturn("com.example.service.UserService.createUser");

            TransactionDefinition childTransactionDefinition = mock(TransactionDefinition.class);
            when(childTransactionDefinition.getName()).thenReturn("org.springframework.data.jpa.repository.support.SimpleJpaRepository.save");
            when(childTransactionDefinition.getIsolationLevel()).thenReturn(TransactionDefinition.ISOLATION_DEFAULT);
            when(childTransactionDefinition.getPropagationBehavior()).thenReturn(TransactionDefinition.PROPAGATION_REQUIRED);

            // Parent Transaction
            txPhaseListener.beforeBegin(transactionDefinition);
            txPhaseListener.afterAcquiredConnection();
            txPhaseListener.afterBegin(null);

            // Child Transaction
            txPhaseListener.beforeBegin(childTransactionDefinition);
            txPhaseListener.afterBegin(null);
            txPhaseListener.executedQuery("INSERT INTO users VALUES (?, ?)");
            txPhaseListener.afterCommit();

            // Back to Parent Transaction
            txPhaseListener.afterCommit();
            txPhaseListener.afterCloseConnection();

            ArgumentCaptor<TransactionLog> parentTxLogCaptor = ArgumentCaptor.forClass(TransactionLog.class);
            verify(txLogListener).listen(parentTxLogCaptor.capture());

            TransactionLog parentTxLog = parentTxLogCaptor.getValue();
            assertNotNull(parentTxLog.getTxId());
            assertEquals("UserService.createUser", parentTxLog.getMethod());

            List<TransactionEvent> transactionEvents = parentTxLog.getEvents();
            assertNotNull(transactionEvents);
            assertEquals(6, transactionEvents.size());
            assertEquals(TransactionEvent.Type.CONNECTION_ACQUIRED, transactionEvents.get(0).getType());
            assertEquals(TransactionEvent.Type.TRANSACTION_START, transactionEvents.get(1).getType());
            assertEquals("Transaction Start [UserService.createUser]", transactionEvents.get(1).getDetails());
            assertEquals(TransactionEvent.Type.TRANSACTION_START, transactionEvents.get(2).getType());
            assertEquals("Transaction Start [SimpleJpaRepository.save]", transactionEvents.get(2).getDetails());
            assertEquals(TransactionEvent.Type.TRANSACTION_END, transactionEvents.get(3).getType());
            assertEquals("Transaction End [SimpleJpaRepository.save]", transactionEvents.get(3).getDetails());
            assertEquals(TransactionEvent.Type.TRANSACTION_END, transactionEvents.get(4).getType());
            assertEquals("Transaction End [UserService.createUser]", transactionEvents.get(4).getDetails());
            assertEquals(TransactionEvent.Type.CONNECTION_RELEASED, transactionEvents.get(5).getType());

            TransactionLog childTxLog = parentTxLog.getChild().get(0);
            assertNull(childTxLog.getTxId());
            assertEquals("SimpleJpaRepository.save", childTxLog.getMethod());
            assertEquals(IsolationLevel.DEFAULT, childTxLog.getIsolation());
            assertEquals(PropagationBehavior.REQUIRED, childTxLog.getPropagation());
            assertTrue(childTxLog.getChild().isEmpty());
            assertEquals(parentTxLog.getThread(), childTxLog.getThread());
            assertEquals(1, childTxLog.getTotalQueryCount());
            assertEquals(1, childTxLog.getExecutedQuires().size());
            assertTrue(childTxLog.getExecutedQuires().contains("INSERT INTO users VALUES (?, ?)"));
            assertNull(childTxLog.getEvents());
        }

        @Test
        void shouldTrackMultipleNestedTransaction() {
            when(transactionDefinition.getName()).thenReturn("com.example.Parent.method");

            TransactionDefinition child1TxDefinition = mock(TransactionDefinition.class);
            when(child1TxDefinition.getName()).thenReturn("com.example.Child1.method");

            TransactionDefinition child2TxDefinition = mock(TransactionDefinition.class);
            when(child2TxDefinition.getName()).thenReturn("com.example.Child2.method");

            // Parent Transaction
            txPhaseListener.beforeBegin(transactionDefinition);
            txPhaseListener.afterAcquiredConnection();
            txPhaseListener.afterBegin(null);

            // Child-1 Transaction
            txPhaseListener.beforeBegin(child1TxDefinition);
            txPhaseListener.afterBegin(null);
            txPhaseListener.afterCommit();

            // Child-2 Transaction
            txPhaseListener.beforeBegin(child2TxDefinition);
            txPhaseListener.afterBegin(null);
            txPhaseListener.afterCommit();

            // Back to Parent Transaction
            txPhaseListener.afterCommit();
            txPhaseListener.afterCloseConnection();

            ArgumentCaptor<TransactionLog> txLogCaptor = ArgumentCaptor.forClass(TransactionLog.class);
            verify(txLogListener).listen(txLogCaptor.capture());

            TransactionLog parentTxLog = txLogCaptor.getValue();
            assertEquals(3, parentTxLog.getTotalTransactionCount());
            assertEquals(2, parentTxLog.getChild().size());
            assertEquals("Parent.method", parentTxLog.getMethod());
            assertEquals("Child1.method", parentTxLog.getChild().get(0).getMethod());
            assertEquals("Child2.method", parentTxLog.getChild().get(1).getMethod());
        }

        @Test
        void shouldTrackMultilevelTransaction() {
            when(transactionDefinition.getName()).thenReturn("com.example.Level1.method");

            TransactionDefinition level2Definition = mock(TransactionDefinition.class);
            when(level2Definition.getName()).thenReturn("com.example.Level2.method");

            TransactionDefinition level3Definition = mock(TransactionDefinition.class);
            when(level3Definition.getName()).thenReturn("com.example.Level3.method");

            // Level-1 Transaction
            txPhaseListener.beforeBegin(transactionDefinition);
            txPhaseListener.afterAcquiredConnection();
            txPhaseListener.afterBegin(null);
            txPhaseListener.executedQuery("INSERT INTO level1 VALUES (?)");

            // Level-2 Transaction
            txPhaseListener.beforeBegin(level2Definition);
            txPhaseListener.afterBegin(null);
            txPhaseListener.executedQuery("UPDATE level2 SET data = ?");

            // Level-3 Transaction
            txPhaseListener.beforeBegin(level3Definition);
            txPhaseListener.afterBegin(null);
            txPhaseListener.executedQuery("SELECT * from level3");
            txPhaseListener.afterCommit();

            // Back to Level-2 Transaction
            txPhaseListener.executedQuery("SELECT * from level2");
            txPhaseListener.afterCommit();

            // Back to Level-1 Transaction
            txPhaseListener.executedQuery("SELECT * from level1");
            txPhaseListener.afterCommit();
            txPhaseListener.afterCloseConnection();

            ArgumentCaptor<TransactionLog> txLogCaptor = ArgumentCaptor.forClass(TransactionLog.class);
            verify(txLogListener).listen(txLogCaptor.capture());
            TransactionLog level1TxLog = txLogCaptor.getValue();

            assertEquals("Level1.method", level1TxLog.getMethod());
            assertEquals(3, level1TxLog.getTotalTransactionCount());
            assertEquals(2, level1TxLog.getExecutedQuires().size());
            assertTrue(level1TxLog.getExecutedQuires().containsAll(List.of("INSERT INTO level1 VALUES (?)", "SELECT * from level1")));
            assertEquals(5, level1TxLog.getTotalQueryCount());
            assertEquals(1, level1TxLog.getChild().size());

            TransactionLog level2TxLog = level1TxLog.getChild().get(0);
            assertEquals(2, level2TxLog.getTotalTransactionCount());
            assertEquals(2, level2TxLog.getExecutedQuires().size());
            assertTrue(level2TxLog.getExecutedQuires().containsAll(List.of("UPDATE level2 SET data = ?", "SELECT * from level2")));
            assertEquals(3, level2TxLog.getTotalQueryCount());
            assertEquals(1, level2TxLog.getChild().size());

            TransactionLog level3TxLog = level2TxLog.getChild().get(0);
            assertEquals(1, level3TxLog.getTotalTransactionCount());
            assertEquals(1, level3TxLog.getExecutedQuires().size());
            assertTrue(level3TxLog.getExecutedQuires().contains("SELECT * from level3"));
            assertEquals(1, level3TxLog.getTotalQueryCount());
            assertTrue(level3TxLog.getChild().isEmpty());
        }


    }

    @Nested
    class ConnectionTrackingTests {

        @Test
        void shouldNotTrackConnectionsWhenNoActiveTransaction() {
            txPhaseListener.afterAcquiredConnection();
            txPhaseListener.executedQuery("SELECT * from test");
            txPhaseListener.afterCloseConnection();

            verifyNoInteractions(txLogListener);
        }

        @Test
        void shouldTrackConnectionAndBuildConnectionSummaryProperly() {
            txBoardProperties.getAlarmingThreshold().setConnection(80L);

            TransactionDefinition innerTxDefinition = mock(TransactionDefinition.class);
            when(innerTxDefinition.getPropagationBehavior()).thenReturn(TransactionDefinition.PROPAGATION_REQUIRES_NEW);

            // Outer Transaction
            txPhaseListener.beforeBegin(transactionDefinition);
            txPhaseListener.afterAcquiredConnection();
            txPhaseListener.afterBegin(null);

            // Inner Transaction
            txPhaseListener.beforeBegin(innerTxDefinition);
            txPhaseListener.afterAcquiredConnection();
            txPhaseListener.afterBegin(null);
            Utils.sleep(50); // Simulate some work for inner transaction
            txPhaseListener.afterCommit();
            txPhaseListener.afterCloseConnection();

            // Back to Outer Transaction
            Utils.sleep(100); // Simulate some work for outer transaction
            txPhaseListener.afterCommit();
            txPhaseListener.afterCloseConnection();

            ArgumentCaptor<TransactionLog> txLogCaptor = ArgumentCaptor.forClass(TransactionLog.class);
            verify(txLogListener).listen(txLogCaptor.capture());

            TransactionLog transactionLog = txLogCaptor.getValue();
            assertTrue(transactionLog.getConnectionOriented());
            ConnectionSummary connectionSummary = transactionLog.getConnectionSummary();
            assertNotNull(connectionSummary);
            assertEquals(2, connectionSummary.acquisitionCount());
            assertEquals(1, connectionSummary.alarmingConnectionCount());
            assertTrue(transactionLog.getHavingAlarmingConnection());

            long occupiedTime = 0L;
            Stack<TransactionEvent> eventStack = new Stack<>();
            for (TransactionEvent event : transactionLog.getEvents()) {
                TransactionEvent.Type type = event.getType();
                if (type == TransactionEvent.Type.CONNECTION_ACQUIRED) {
                    eventStack.push(event);
                } else if (type == TransactionEvent.Type.CONNECTION_RELEASED) {
                    TransactionEvent prev = eventStack.pop();
                    occupiedTime += Duration.between(prev.getTimestamp(), event.getTimestamp()).toMillis();
                }
            }

            assertEquals(occupiedTime, connectionSummary.occupiedTime());
        }
    }

    @Nested
    class IsolationAndPropagationBehaviorTests {

        @ParameterizedTest
        @EnumSource(IsolationLevel.class)
        void shouldHandleAllIsolationLevels(IsolationLevel isolation) {
            when(transactionDefinition.getIsolationLevel()).thenReturn(isolation.value());

            txPhaseListener.beforeBegin(transactionDefinition);
            txPhaseListener.afterAcquiredConnection();
            txPhaseListener.afterBegin(null);
            txPhaseListener.afterCommit();
            txPhaseListener.afterCloseConnection();

            ArgumentCaptor<TransactionLog> txLogCaptor = ArgumentCaptor.forClass(TransactionLog.class);
            verify(txLogListener).listen(txLogCaptor.capture());

            TransactionLog transactionLog = txLogCaptor.getValue();
            assertEquals(isolation, transactionLog.getIsolation());
        }

        @ParameterizedTest
        @EnumSource(PropagationBehavior.class)
        void shouldHandleAllPropagationBehaviors(PropagationBehavior propagation) {
            when(transactionDefinition.getPropagationBehavior()).thenReturn(propagation.value());

            txPhaseListener.beforeBegin(transactionDefinition);
            txPhaseListener.afterBegin(null);
            txPhaseListener.afterCommit();

            ArgumentCaptor<TransactionLog> txLogCaptor = ArgumentCaptor.forClass(TransactionLog.class);
            verify(txLogListener).listen(txLogCaptor.capture());

            TransactionLog transactionLog = txLogCaptor.getValue();
            assertEquals(propagation, transactionLog.getPropagation());
        }
    }

    @Nested
    class TransactionListenerErrorHandlingTests {

        @Test
        void shouldHandleListenerExceptionsGracefully() {
            TransactionLogListener failingListener = mock(TransactionLogListener.class);
            doThrow(new RuntimeException("Listener failed!")).when(failingListener).listen(any());

            TransactionPhaseListenerImpl listenerWithFailingCallback = new TransactionPhaseListenerImpl(
                    List.of(txLogListener, failingListener), txBoardProperties);

            assertDoesNotThrow(() -> {
                listenerWithFailingCallback.beforeBegin(transactionDefinition);
                listenerWithFailingCallback.afterBegin(null);
                listenerWithFailingCallback.afterAcquiredConnection();
                listenerWithFailingCallback.afterCloseConnection();
                listenerWithFailingCallback.afterCommit();
            });

            // Should still call the working listener
            verify(txLogListener).listen(any(TransactionLog.class));

            // Should log the error
            List<ILoggingEvent> loggingEvents = loggingEventAppender.list;
            assertTrue(loggingEvents.stream().anyMatch(e -> e.getLevel() == Level.ERROR &&
                    e.getMessage().contains("Failed to publish transaction log to listener")
            ));
        }

        @Test
        void shouldHandleEmptyListenersList() {
            TransactionPhaseListenerImpl listenerWithEmptyCallbacks = new TransactionPhaseListenerImpl(
                    List.of(), txBoardProperties);

            assertDoesNotThrow(() -> {
                listenerWithEmptyCallbacks.beforeBegin(transactionDefinition);
                listenerWithEmptyCallbacks.afterBegin(null);
                listenerWithEmptyCallbacks.afterAcquiredConnection();
                listenerWithEmptyCallbacks.afterCloseConnection();
                listenerWithEmptyCallbacks.afterCommit();
            });
        }

        @Test
        void shouldHandleNullListenersList() {
            TransactionPhaseListenerImpl listenerWithoutCallbacks = new TransactionPhaseListenerImpl(
                    null, txBoardProperties);

            assertDoesNotThrow(() -> {
                listenerWithoutCallbacks.beforeBegin(transactionDefinition);
                listenerWithoutCallbacks.afterBegin(null);
                listenerWithoutCallbacks.afterAcquiredConnection();
                listenerWithoutCallbacks.afterCloseConnection();
                listenerWithoutCallbacks.afterCommit();
            });
        }

    }

    @Nested
    class ConcurrencyTests {

        @Test
        void shouldHandleConcurrentTransactionsInDifferentThreads() throws InterruptedException {
            int threadCount = 5;
            CountDownLatch latch = new CountDownLatch(threadCount);
            ExecutorService executor = Executors.newFixedThreadPool(threadCount);

            for (int i = 0; i < threadCount; i++) {
                executor.execute(() -> {
                    try {
                        txPhaseListener.beforeBegin(transactionDefinition);
                        txPhaseListener.afterAcquiredConnection();
                        txPhaseListener.afterBegin(null);
                        Utils.sleep(10); // Simulate work
                        txPhaseListener.afterCommit();
                        txPhaseListener.afterCloseConnection();
                    } catch (Exception e) {
                        Thread.currentThread().interrupt();
                    } finally {
                        latch.countDown();
                    }
                });
            }

            latch.await();
            executor.shutdown();

            verify(txLogListener, times(threadCount)).listen(any(TransactionLog.class));
        }

        @Test
        void shouldIsolateTransactionStatesBetweenThreads() throws InterruptedException {
            when(transactionDefinition.getName()).thenReturn("com.example.Thread1Service.method");

            TransactionDefinition transactionDefinition2 = mock(TransactionDefinition.class);
            when(transactionDefinition2.getName()).thenReturn("com.example.Thread2Service.method");

            CountDownLatch latch = new CountDownLatch(2);

            Thread thread1 = new Thread(() -> {
                try {
                    txPhaseListener.beforeBegin(transactionDefinition);
                    txPhaseListener.afterBegin(null);
                    txPhaseListener.afterAcquiredConnection();
                    txPhaseListener.executedQuery("SELECT * FROM thread1_table");
                    txPhaseListener.afterCloseConnection();
                    txPhaseListener.afterCommit();
                } catch (Exception e) {
                    Thread.currentThread().interrupt();
                } finally {
                    latch.countDown();
                }
            }, "thread1");

            Thread thread2 = new Thread(() -> {
                try {
                    txPhaseListener.beforeBegin(transactionDefinition2);
                    txPhaseListener.afterBegin(null);
                    txPhaseListener.afterAcquiredConnection();
                    txPhaseListener.executedQuery("INSERT INTO thread2_table VALUES(?, ?)");
                    txPhaseListener.executedQuery("SELECT * FROM thread2_table");
                    txPhaseListener.afterCloseConnection();
                    txPhaseListener.afterCommit();
                } catch (Exception e) {
                    Thread.currentThread().interrupt();
                } finally {
                    latch.countDown();
                }
            }, "thread2");

            thread1.start();
            thread2.start();
            latch.await();

            verify(txLogListener, times(2)).listen(any(TransactionLog.class));

            ArgumentCaptor<TransactionLog> txLogCaptor = ArgumentCaptor.forClass(TransactionLog.class);
            verify(txLogListener, times(2)).listen(txLogCaptor.capture());

            Map<String, TransactionLog> transactionLogMap = txLogCaptor.getAllValues()
                    .stream()
                    .collect(Collectors.toMap(TransactionLog::getThread, Function.identity()));

            TransactionLog thread1TxLog = transactionLogMap.get("thread1");
            assertEquals("Thread1Service.method", thread1TxLog.getMethod());
            assertEquals("thread1", thread1TxLog.getThread());
            assertEquals(1, thread1TxLog.getTotalQueryCount());

            TransactionLog thread2TxLog = transactionLogMap.get("thread2");
            assertEquals("Thread2Service.method", thread2TxLog.getMethod());
            assertEquals("thread2", thread2TxLog.getThread());
            assertEquals(2, thread2TxLog.getTotalQueryCount());
        }
    }
}
