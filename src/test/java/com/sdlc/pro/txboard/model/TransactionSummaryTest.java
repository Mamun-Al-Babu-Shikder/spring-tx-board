package com.sdlc.pro.txboard.model;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

class TransactionSummaryTest {

    @Test
    void testGetters() {
        TransactionSummary summary = new TransactionSummary(
                10, 5, 2, 1000, 3, 7, 700, 2
        );

        assertEquals(10, summary.getCommittedCount());
        assertEquals(5, summary.getRolledBackCount());
        assertEquals(2, summary.getErroredCount());
        assertEquals(1000, summary.getTotalDuration());
        assertEquals(3, summary.getAlarmingCount());
        assertEquals(7, summary.getConnectionAcquisitionCount());
        assertEquals(700, summary.getTotalConnectionOccupiedTime());
        assertEquals(2, summary.getAlarmingConnectionCount());
    }

    @Test
    void testGetTotalTransaction() {
        TransactionSummary summary = new TransactionSummary(
                10, 5, 2, 1000, 3, 7, 700, 2
        );

        assertEquals(17, summary.getTotalTransaction());
    }

    @Test
    void testGetAverageDurationNormalCase() {
        TransactionSummary summary = new TransactionSummary(
                10, 5, 5, 2000, 3, 7, 700, 2
        );

        // totalTransactions = 20, totalDuration = 2000
        assertEquals(100.0, summary.getAverageDuration());
    }

    @Test
    void testGetAverageConnectionOccupiedTimeNormalCase() {
        TransactionSummary summary = new TransactionSummary(
                10, 5, 5, 2000, 3, 4, 800, 2
        );

        // totalConnectionOccupiedTime = 800, connectionAcquisitionCount = 4
        assertEquals(200.0, summary.getAverageConnectionOccupiedTime());
    }

    @Test
    void testGetAverageDurationWhenNoTransactions() {
        TransactionSummary summary = new TransactionSummary(
                0, 0, 0, 2000, 3, 5, 500, 1
        );

        assertEquals(0.0, summary.getAverageDuration());
    }

    @Test
    void testGetAverageConnectionOccupiedTimeWhenNoConnections() {
        TransactionSummary summary = new TransactionSummary(
                1, 1, 1, 2000, 3, 0, 500, 1
        );

        assertEquals(0.0, summary.getAverageConnectionOccupiedTime());
    }

    @Test
    void testAllZeroValues() {
        TransactionSummary summary = new TransactionSummary(
                0, 0, 0, 0, 0, 0, 0, 0
        );

        assertEquals(0, summary.getCommittedCount());
        assertEquals(0, summary.getRolledBackCount());
        assertEquals(0, summary.getErroredCount());
        assertEquals(0, summary.getTotalDuration());
        assertEquals(0, summary.getAlarmingCount());
        assertEquals(0, summary.getConnectionAcquisitionCount());
        assertEquals(0, summary.getTotalConnectionOccupiedTime());
        assertEquals(0, summary.getAlarmingConnectionCount());

        assertEquals(0, summary.getTotalTransaction());
        assertEquals(0.0, summary.getAverageDuration());
        assertEquals(0.0, summary.getAverageConnectionOccupiedTime());
    }
}
