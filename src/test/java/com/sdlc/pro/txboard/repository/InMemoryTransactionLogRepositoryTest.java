package com.sdlc.pro.txboard.repository;

import com.sdlc.pro.txboard.config.TxBoardProperties;
import com.sdlc.pro.txboard.enums.TransactionPhaseStatus;
import com.sdlc.pro.txboard.model.TransactionLog;
import com.sdlc.pro.txboard.model.TransactionSummary;
import com.sdlc.pro.txboard.util.TxLogUtils;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class InMemoryTransactionLogRepositoryTest {
    private static TransactionLogRepository logRepository;

    @BeforeAll
    static void setup() {
        TxBoardProperties properties = new TxBoardProperties();
        logRepository = new InMemoryTransactionLogRepository(properties);
        for (TransactionLog transactionLog : TxLogUtils.createTestTransactionLogs()) {
            logRepository.save(transactionLog);
        }
    }

    @Test
    void testTransactionSummary() {
        TransactionSummary transactionSummary = logRepository.getTransactionSummary();
        assertEquals(6L, transactionSummary.getTotalTransaction());
        assertEquals(4L, transactionSummary.getCommittedCount());
        assertEquals(1L, transactionSummary.getRolledBackCount());
        assertEquals(1L, transactionSummary.getErroredCount());
        assertEquals(1L, transactionSummary.getAlarmingCount());
        assertEquals(4L, transactionSummary.getConnectionAcquisitionCount());
        assertEquals(1L, transactionSummary.getAlarmingConnectionCount());

        assertEquals(5515L, transactionSummary.getTotalDuration());
        assertEquals(919.0, Math.floor(transactionSummary.getAverageDuration()));
        assertEquals(5500L, transactionSummary.getTotalConnectionOccupiedTime());
        assertEquals(1375.0, transactionSummary.getAverageConnectionOccupiedTime());
    }

    @Test
    void testCount() {
        long totalTransactionLog = logRepository.count();
        assertEquals(6L, totalTransactionLog);
    }

    @Test
    void testCountByTransactionStatus() {
        long committedCount = logRepository.countByTransactionStatus(TransactionPhaseStatus.COMMITTED);
        long rolledBackCount = logRepository.countByTransactionStatus(TransactionPhaseStatus.ROLLED_BACK);
        long erroredCount = logRepository.countByTransactionStatus(TransactionPhaseStatus.ERRORED);

        assertEquals(4L, committedCount);
        assertEquals(1L, rolledBackCount);
        assertEquals(1L, erroredCount);
    }
}
