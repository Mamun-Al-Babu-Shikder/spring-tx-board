package com.sdlc.pro.txboard.listener;

import com.sdlc.pro.txboard.config.TxBoardProperties;
import com.sdlc.pro.txboard.enums.TransactionStatus;
import com.sdlc.pro.txboard.model.TransactionLog;
import com.sdlc.pro.txboard.repository.TransactionLogRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.transaction.TransactionExecution;
import org.springframework.transaction.TransactionExecutionListener;

import java.time.Instant;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

public class TransactionMonitoringListener implements TransactionExecutionListener {
    private static final Logger log = LoggerFactory.getLogger(TransactionMonitoringListener.class);

    private final ConcurrentMap<Integer, TransactionInfo> transactionInfoConcurrentMap;
    private final TransactionLogRepository transactionLogRepository;
    private final TxBoardProperties txBoardProperties;

    public TransactionMonitoringListener(TransactionLogRepository transactionLogRepository, TxBoardProperties txBoardProperties) {
        this.transactionInfoConcurrentMap = new ConcurrentHashMap<>();
        this.transactionLogRepository = transactionLogRepository;
        this.txBoardProperties = txBoardProperties;
    }

    @Override
    public void afterBegin(TransactionExecution transaction, Throwable beginFailure) {
        transactionInfoConcurrentMap.put(
                transaction.hashCode(),
                new TransactionInfo(Instant.now(), extractTransactionMethod(transaction))
        );
    }

    @Override
    public void afterCommit(TransactionExecution transaction, Throwable commitFailure) {
        this.saveTransactionLog(transaction, TransactionStatus.COMMITTED);
    }

    @Override
    public void afterRollback(TransactionExecution transaction, Throwable rollbackFailure) {
        this.saveTransactionLog(transaction, TransactionStatus.ROLLED_BACK);
    }

    private void saveTransactionLog(TransactionExecution transaction, TransactionStatus status) {
        TransactionInfo info = transactionInfoConcurrentMap.remove(transaction.hashCode());
        if (info != null) {
            String thread = Thread.currentThread().getName();
            TransactionLog transactionLog = TransactionLog.from(info.methodName(), info.startTime(), Instant.now(), status, thread, txBoardProperties.getAlarmingThreshold());
            transactionLogRepository.save(transactionLog);

            if (txBoardProperties.isEnableListenerLog()) {
                if (transactionLog.isAlarming()) {
                    log.warn("Slow transaction tx-[{}] method={} status={} started={} ended={} duration={}ms",
                            transactionLog.getTxId(), transactionLog.getMethod(), transactionLog.getStatus(),
                            transactionLog.getStartTime(), transactionLog.getEndTime(), transactionLog.getDuration());
                } else {
                    log.info("Transaction tx-[{}] method={} status={} started={} ended={} duration={}ms",
                            transactionLog.getTxId(), transactionLog.getMethod(), transactionLog.getStatus(),
                            transactionLog.getStartTime(), transactionLog.getEndTime(), transactionLog.getDuration());
                }
            }
        }
    }

    private String extractTransactionMethod(TransactionExecution transaction) {
        String txName = transaction.getTransactionName();
        String[] strings = txName.split("\\.");
        int len = strings.length;
        return len < 2 ? "anonymous" : strings[len - 2] + "." + strings[strings.length - 1];
    }

    private record TransactionInfo(Instant startTime, String methodName) {

    }
}
