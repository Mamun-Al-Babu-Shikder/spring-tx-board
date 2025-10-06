package com.sdlc.pro.txboard.listener;

import com.sdlc.pro.txboard.config.TxBoardProperties;
import com.sdlc.pro.txboard.enums.IsolationLevel;
import com.sdlc.pro.txboard.enums.PropagationBehavior;
import com.sdlc.pro.txboard.enums.TransactionPhaseStatus;
import com.sdlc.pro.txboard.model.ConnectionSummary;
import com.sdlc.pro.txboard.model.TransactionEvent;
import com.sdlc.pro.txboard.model.TransactionLog;
import com.sdlc.pro.txboard.util.NPlusOneAnalyzer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.transaction.TransactionDefinition;

import java.time.Duration;
import java.time.Instant;
import java.util.Deque;
import java.util.LinkedList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;

import static com.sdlc.pro.txboard.config.TxBoardProperties.AlarmingThreshold;

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
    public void afterBegin(Throwable throwable) {
        Instant timestamp = Instant.now();
        currentTransactionInfo().ifPresent(txInfo -> txInfo.setStartTime(timestamp));
        addTransactionEvent(new TransactionEvent(TransactionEvent.Type.TRANSACTION_START, timestamp, "Transaction Start [%s]".formatted(currentTransactionMethodName())));
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
            currentTransactionInfo().ifPresent(txInfo -> {
                if (txInfo.isMostParent() && txInfo.isCompleted()) {
                    txInfo.addPostTransactionQuery(query);
                } else {
                    txInfo.addExecutedQuery(query);
                }
            });
        }
    }

    @Override
    public void errorOccurredAtTransactionPhase(Throwable throwable) {
        endWithTransactionStatusAndEvent(TransactionPhaseStatus.ERRORED);
    }

    private void endWithTransactionStatusAndEvent(TransactionPhaseStatus status) {
        currentTransactionInfo().ifPresent(txInfo -> {
            Instant timestamp = Instant.now();
            txInfo.setEndTime(timestamp);
            txInfo.setStatus(status);
            addTransactionEvent(new TransactionEvent(TransactionEvent.Type.TRANSACTION_END, timestamp, "Transaction End [%s]".formatted(txInfo.getMethodName())));

            if (txInfo.isMostParent()) {
                if (!hasActiveConnection()) {
                    finish();
                }
            } else {
                popCurrentTransactionInfo();
            }
        });
    }

    private void finish() {
        popCurrentTransactionInfo().ifPresent(txInfo -> {
            TransactionLog txLog = txInfo.toTransactionLog();

            boolean healthyTransaction = txLog.isHealthyTransaction();
            boolean detailedLoggingEnabled = txBoardProperties.getLogType() == TxBoardProperties.LogType.DETAILS;

            int acquiredConnections = getConnectionAcquiredCount(txLog);

            boolean nPlusOne = txLog.isNPlusOneDetected();
            if (detailedLoggingEnabled) {
                String message = buildDetailedLogMessage(txLog, acquiredConnections);
                if (healthyTransaction && !nPlusOne) {
                    log.info("{}", message);
                } else {
                    log.warn("{}", message);
                }
            } else {
                if (healthyTransaction && !nPlusOne) {
                    log.info("Transaction [{}] took {} ms, Status: {}", txLog.getMethod(), txLog.getDuration(), txLog.getStatus());
                } else {
                    log.warn("Transaction [{}] took {} ms, Status: {}, Connections: {}, Queries: {}",
                            txLog.getMethod(), txLog.getDuration(), txLog.getStatus(), acquiredConnections, txLog.getTotalQueryCount());
                }
            }

            if (nPlusOne) {
                log.warn("[TX-Board] Potential N+1 query pattern detected in transaction [{}]. Repeated child queries observed.", txLog.getMethod());
            }

            this.publishTransactionLogToListeners(txLog);
        });
    }

    private static int getConnectionAcquiredCount(TransactionLog txLog) {
        return txLog.getConnectionSummary() != null ? txLog.getConnectionSummary().acquisitionCount() : 0;
    }

    private static String buildDetailedLogMessage(TransactionLog txLog, int acquiredConnections) {
        String base = """
                [TX-Board] Transaction Completed:
                  • ID: %s
                  • Method: %s
                  • Status: %s
                  • Duration: %d ms
                  • Connections Acquired: %d
                  • Queries Executed: %d
                  • Started At: %s
                  • Ended At: %s
                """.formatted(
                txLog.getTxId(),
                txLog.getMethod(),
                txLog.getStatus(),
                txLog.getDuration(),
                acquiredConnections,
                txLog.getTotalQueryCount(),
                txLog.getStartTime(),
                txLog.getEndTime()
        );

        if (txLog.getChild().isEmpty()) {
            return base;
        }

        return base + "  • Inner Transactions:\n" + formatNestedTransactions(txLog);
    }

    private static String formatNestedTransactions(TransactionLog txLog) {
        StringBuilder nestedTx = new StringBuilder();
        appendChildren(txLog, nestedTx, "    ");
        return nestedTx.toString();
    }

    private static void appendChildren(TransactionLog txLog, StringBuilder nestedTx, String prefix) {
        if (txLog.getChild().isEmpty()) {
            return;
        }

        List<TransactionLog> children = txLog.getChild();
        for (int i = 0; i < children.size(); i++) {
            TransactionLog child = children.get(i);
            boolean last = (i == children.size() - 1);

            nestedTx.append(prefix)
                    .append(last ? "└── " : "├── ")
                    .append(child.getMethod())
                    .append(" (")
                    .append(child.getDuration())
                    .append(" ms, ")
                    .append(child.getStatus())
                    .append(")\n");

            String childPrefix = prefix + (last ? "    " : "│   ");
            appendChildren(child, nestedTx, childPrefix);
        }
    }

    private void publishTransactionLogToListeners(TransactionLog txLog) {
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


    private int onConnectionAcquired() {
        int count = activeConnectionCount.get() + 1;
        activeConnectionCount.set(count);
        return count;
    }

    private int onConnectionReleased() {
        int count = activeConnectionCount.get();
        activeConnectionCount.set(count - 1);
        return count;
    }

    private boolean hasActiveConnection() {
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
        private final AlarmingThreshold alarmingThreshold;
        private final List<String> postTransactionQuires;

        public TransactionInfo(String methodName, PropagationBehavior propagation, IsolationLevel isolation,
                               AlarmingThreshold alarmingThreshold, boolean isMostParent) {
            this.isMostParent = isMostParent;
            this.txId = this.isMostParent ? ATOMIC_TX_ID_GEN.incrementAndGet() : null;
            this.methodName = methodName;
            this.propagation = propagation;
            this.isolation = isolation;
            this.thread = Thread.currentThread().getName();
            this.child = new LinkedList<>();
            this.executedQuires = new LinkedList<>();
            this.events = this.isMostParent ? new LinkedList<>() : null;
            this.alarmingThreshold = alarmingThreshold;
            this.postTransactionQuires = this.isMostParent ? new LinkedList<>() : null;
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

        public void addPostTransactionQuery(String query) {
            this.postTransactionQuires.add(query);
        }

        public TransactionLog toTransactionLog() {
            List<TransactionLog> child = this.child.stream()
                    .map(TransactionInfo::toTransactionLog)
                    .toList();

            ConnectionSummary connectionSummary = this.isMostParent ? getConnectionRelatedInfo() : null;

            // Aggregate executed queries from this node and children for N+1 analysis
            List<String> allQueries = new LinkedList<>(this.executedQuires);
            for (TransactionLog c : child) {
                allQueries.addAll(c.getExecutedQuires());
            }
            boolean nPlusOne = NPlusOneAnalyzer.detectPotentialNPlusOne(allQueries);

            return new TransactionLog(
                    this.txId,
                    this.methodName,
                    this.propagation,
                    this.isolation,
                    this.startTime,
                    this.endTime,
                    connectionSummary,
                    this.status,
                    this.thread,
                    executedQuires,
                    child,
                    events,
                    this.alarmingThreshold.getTransaction(),
                    nPlusOne,
                    this.postTransactionQuires
            );
        }

        private ConnectionSummary getConnectionRelatedInfo() {
            long occupiedTime = 0L;
            int acquisitionCount = 0;
            int alarmingConnectionCount = 0;

            if (this.events != null && !this.events.isEmpty()) {
                Deque<TransactionEvent> conEventStack = new LinkedList<>();
                for (TransactionEvent event : this.events) {
                    if (event.getType() == TransactionEvent.Type.CONNECTION_ACQUIRED) {
                        acquisitionCount++;
                        conEventStack.push(event);
                    } else if (event.getType() == TransactionEvent.Type.CONNECTION_RELEASED) {
                        TransactionEvent prevEvent = conEventStack.pop();
                        long duration = Duration.between(prevEvent.getTimestamp(), event.getTimestamp()).toMillis();
                        if (duration >= alarmingThreshold.getConnection()) {
                            alarmingConnectionCount++;
                        }
                        occupiedTime += duration;
                    }
                }
            }

            return new ConnectionSummary(acquisitionCount, alarmingConnectionCount, occupiedTime);
        }
    }
}
