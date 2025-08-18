package com.sdlc.pro.txboard.listener;

import com.sdlc.pro.txboard.config.TxBoardProperties;
import com.sdlc.pro.txboard.enums.IsolationLevel;
import com.sdlc.pro.txboard.enums.PropagationBehavior;
import com.sdlc.pro.txboard.enums.TransactionPhaseStatus;
import com.sdlc.pro.txboard.model.TransactionEvent;
import com.sdlc.pro.txboard.model.TransactionLog;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.TransactionStatus;

import java.time.Duration;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;

public final class TransactionPhaseListenerImpl implements TransactionPhaseListener {
    private static final Logger log = LoggerFactory.getLogger(TransactionPhaseListenerImpl.class);
    private static final ThreadLocal<Deque<TransactionInfo>> txInfoThreadLocal = ThreadLocal.withInitial(LinkedList::new);
    private static final ThreadLocal<Integer> activeConnectionCount = ThreadLocal.withInitial(() -> 0);
    private final List<TransactionLogListener> transactionLogListeners;
    private final TxBoardProperties txBoardProperties;

    public TransactionPhaseListenerImpl(List<TransactionLogListener> transactionLogListeners, TxBoardProperties txBoardProperties) {
        this.transactionLogListeners = transactionLogListeners;
        this.txBoardProperties = txBoardProperties;
    }

    @Override
    public void beforeBegin(TransactionDefinition definition) {
        String method = extractTransactionMethod(definition);
        PropagationBehavior propagation = PropagationBehavior.of(definition.getPropagationBehavior());
        IsolationLevel isolation = IsolationLevel.of(definition.getIsolationLevel());
        TransactionInfo newTxInfo = new TransactionInfo(
                method,
                propagation,
                isolation,
                txBoardProperties.getAlarmingThreshold(),
                !hasActiveTransaction()
        );

        currentTransactionInfo().ifPresent(oldTxInfo -> buildRelation(oldTxInfo, newTxInfo));
        trackNewTransactionInfo(newTxInfo);
    }

    private static void buildRelation(TransactionInfo oldTxInfo, TransactionInfo newTxInfo) {
        oldTxInfo.addChild(newTxInfo);
    }

    @Override
    public void afterBegin(TransactionStatus transactionStatus, Throwable throwable) {
        currentTransactionInfo().ifPresent(txInfo -> txInfo.setStartTime(Instant.now()));
        addTransactionEvent(new TransactionEvent(TransactionEvent.Type.TRANSACTION_START, "Transaction Start [%s]".formatted(currentTransactionMethodName())));
        if (throwable != null) {
            endWithTransactionStatusAndEvent(TransactionPhaseStatus.ERRORED);
        }
    }

    @Override
    public void afterCommit() {
        endWithTransactionStatusAndEvent(TransactionPhaseStatus.COMMITTED);
    }

    @Override
    public void afterRollback() {
        endWithTransactionStatusAndEvent(TransactionPhaseStatus.ROLLED_BACK);
    }

    @Override
    public void afterAcquiredConnection() {
        if (hasActiveTransaction()) {
            int count = onConnectionAcquired();
            addTransactionEvent(new TransactionEvent(TransactionEvent.Type.CONNECTION_ACQUIRED, "Connection Acquired [%d]".formatted(count)));
        }
    }

    @Override
    public void afterCloseConnection() {
        if (hasActiveTransaction()) {
            int count = onConnectionReleased();
            addTransactionEvent(new TransactionEvent(TransactionEvent.Type.CONNECTION_RELEASED, "Connection Released [%d]".formatted(count)));
            if (getParentTransactionInfo().isCompleted()) {
                finish();
            }
        }
    }

    @Override
    public void executedQuery(String query) {
        if (hasActiveConnection()) {
            currentTransactionInfo().ifPresent(txInfo -> txInfo.addExecutedQuery(query));
        }
    }

    @Override
    public void errorOccurredAtTransactionPhase(Throwable throwable) {
        endWithTransactionStatusAndEvent(TransactionPhaseStatus.ERRORED);
    }

    private void endWithTransactionStatusAndEvent(TransactionPhaseStatus status) {
        currentTransactionInfo().ifPresent(txInfo -> {
            txInfo.setEndTime(Instant.now());
            txInfo.setStatus(status);
            addTransactionEvent(new TransactionEvent(TransactionEvent.Type.TRANSACTION_END, "Transaction End [%s]".formatted(txInfo.getMethodName())));

            if (txInfo.isMostParent()) {
                if (!hasActiveConnection()) {
                    finish();
                }
            } else {
                popCurrentTransactionInfo();
            }

            // TODO: we can log the inner transactions according to the configuration
        });
    }

    private void finish() {
        popCurrentTransactionInfo().ifPresent(txInfo -> {
            TransactionLog txLog = txInfo.toTransactionLog();
            String logMsg = String.format(
                    """
                            [TX-Board] Transaction Completed:
                              • ID: %s
                              • Method: %s
                              • Status: %s
                              • Duration: %s ms
                              • Connections Acquired: %s
                              • Queries Executed: %s
                              • Started At: %s
                              • Ended At: %s""",
                    txLog.getTxId(),
                    txLog.getMethod(),
                    txLog.getStatus(),
                    txLog.getDuration(),
                    txLog.getConnectionAcquisitionCount(),
                    txLog.getTotalQueryCount(),
                    txLog.getStartTime(),
                    txLog.getEndTime()
            );

            switch (txBoardProperties.getLogLevel()) {
                case TRACE -> log.trace(logMsg);
                case DEBUG -> log.debug(logMsg);
                case INFO -> log.info(logMsg);
                case WARN -> log.warn(logMsg);
                case ERROR -> log.error(logMsg);
                case OFF -> {}
            }

            this.publishTransactionLogToListeners(txInfo);
        });
    }

    private void publishTransactionLogToListeners(TransactionInfo txInfo) {
        TransactionLog txLog = txInfo.toTransactionLog();
        if (this.transactionLogListeners != null && !this.transactionLogListeners.isEmpty()) {
            for (TransactionLogListener logListener : this.transactionLogListeners) {
                try {
                    logListener.listen(txLog);
                } catch (Exception ex) {
                    log.error("Failed to publish transaction log to listener: {}, Ex: {}", logListener.getClass().getName(), ex.getMessage());
                }
            }
        }
    }

    private static boolean hasActiveTransaction() {
        return !txInfoThreadLocal.get().isEmpty();
    }

    private static void trackNewTransactionInfo(TransactionInfo transactionInfo) {
        txInfoThreadLocal.get().push(transactionInfo);
    }

    private static String currentTransactionMethodName() {
        return currentTransactionInfo()
                .map(TransactionInfo::getMethodName)
                .orElse("anonymous");
    }

    private static Optional<TransactionInfo> currentTransactionInfo() {
        Deque<TransactionInfo> txInfoDeque = txInfoThreadLocal.get();
        return Optional.ofNullable(txInfoDeque.isEmpty() ? null : txInfoDeque.peek());
    }

    private static Optional<TransactionInfo> popCurrentTransactionInfo() {
        Deque<TransactionInfo> txInfoDeque = txInfoThreadLocal.get();
        return Optional.ofNullable(txInfoDeque.isEmpty() ? null : txInfoDeque.pop());
    }


    public int onConnectionAcquired() {
        int count = activeConnectionCount.get() + 1;
        activeConnectionCount.set(count);
        return count;
    }

    public int onConnectionReleased() {
        int count = activeConnectionCount.get();
        activeConnectionCount.set(count - 1);
        return count;
    }

    public boolean hasActiveConnection() {
        return activeConnectionCount.get() > 0;
    }

    private void addTransactionEvent(TransactionEvent event) {
        this.getParentTransactionInfo().addEvent(event);
    }

    private TransactionInfo getParentTransactionInfo() {
        return txInfoThreadLocal.get().getLast();
    }

    private static String extractTransactionMethod(TransactionDefinition definition) {
        String txName = definition.getName();
        if (txName == null) {
            return "anonymous";
        }
        String[] strings = txName.split("\\.");
        int len = strings.length;
        return len < 2 ? "anonymous" : strings[len - 2] + "." + strings[strings.length - 1];
    }

    private static class TransactionInfo {
        private static final AtomicInteger ATOMIC_TX_ID_GEN = new AtomicInteger(0);

        private final Integer txId;
        private final boolean isMostParent;
        private final String methodName;
        private final PropagationBehavior propagation;
        private final IsolationLevel isolation;
        private Instant startTime;
        private Instant endTime;
        private TransactionPhaseStatus status;
        private final String thread;
        private final List<TransactionInfo> child;
        private final List<String> executedQuires;
        private final List<TransactionEvent> events;
        private final long alarmingThreshold;

        public TransactionInfo(String methodName, PropagationBehavior propagation, IsolationLevel isolation, long alarmingThreshold, boolean isMostParent) {
            this.txId = ATOMIC_TX_ID_GEN.incrementAndGet();
            this.isMostParent = isMostParent;
            this.methodName = methodName;
            this.propagation = propagation;
            this.isolation = isolation;
            this.thread = Thread.currentThread().getName();
            this.child = new LinkedList<>();
            this.executedQuires = new LinkedList<>();
            this.events = new LinkedList<>();
            this.alarmingThreshold = alarmingThreshold;
        }

        public String getMethodName() {
            return methodName;
        }

        public void setStartTime(Instant startTime) {
            this.startTime = startTime;
        }

        public void setEndTime(Instant endTime) {
            this.endTime = endTime;
        }

        public boolean isCompleted() {
            return this.endTime != null;
        }

        public TransactionPhaseStatus getStatus() {
            return status;
        }

        public void setStatus(TransactionPhaseStatus status) {
            this.status = status;
        }

        public long getTransactionDuration() {
            return this.startTime == null || this.endTime == null ? 0L : Duration.between(this.startTime, this.endTime).toMillis();
        }

        public void addChild(TransactionInfo childTxInfo) {
            this.child.add(childTxInfo);
        }

        public void addExecutedQuery(String query) {
            this.executedQuires.add(query);
        }

        public void addEvent(TransactionEvent event) {
            this.events.add(event);
        }

        public boolean isMostParent() {
            return this.isMostParent;
        }

        public TransactionLog toTransactionLog() {
            List<TransactionLog> child = this.child.stream()
                    .map(TransactionInfo::toTransactionLog)
                    .toList();

            int connectionAcquisitionCount = (int) calculateConnectionAcquisitionCount();
            long occupiedTime = calculateConnectionOccupiedTime();

            return new TransactionLog(
                    this.txId,
                    this.methodName,
                    this.propagation,
                    this.isolation,
                    this.startTime,
                    this.endTime,
                    connectionAcquisitionCount,
                    occupiedTime,
                    this.alarmingThreshold,
                    this.status,
                    this.thread,
                    child,
                    executedQuires,
                    events
            );
        }

        private long calculateConnectionOccupiedTime() {
            if (this.events == null || this.events.isEmpty()) {
                return 0L;
            }

            long occupiedTime = 0L;
            Deque<TransactionEvent> conEventStack = new LinkedList<>();;
            for (TransactionEvent event : this.events) {
                if (event.getType() == TransactionEvent.Type.CONNECTION_ACQUIRED) {
                    conEventStack.push(event);
                } else if (event.getType() == TransactionEvent.Type.CONNECTION_RELEASED) {
                    TransactionEvent prevEvent = conEventStack.pop();
                    occupiedTime += Duration.between(prevEvent.getTimestamp(), event.getTimestamp()).toMillis();
                }
            }

            return occupiedTime;
        }

        private long calculateConnectionAcquisitionCount() {
            if (this.events == null || this.events.isEmpty()) {
                return 0;
            }

            return this.events.stream()
                    .filter(e-> e.getType() == TransactionEvent.Type.CONNECTION_ACQUIRED)
                    .count();
        }
    }
}

