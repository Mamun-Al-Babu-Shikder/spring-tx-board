package com.sdlc.pro.txboard.model;

public class TransactionSummary {
    private final long committedCount;
    private final long rolledBackCount;
    private final long erroredCount;
    private final long totalDuration;
    private final long alarmingCount;
    private final long connectionAcquisitionCount;
    private final long totalConnectionOccupiedTime;
    private final long alarmingConnectionCount;

    public TransactionSummary(long committedCount, long rolledBackCount, long erroredCount, long totalDuration,
                              long alarmingCount, long connectionAcquisitionCount,
                              long totalConnectionOccupiedTime, long alarmingConnectionCount) {
        this.committedCount = committedCount;
        this.rolledBackCount = rolledBackCount;
        this.erroredCount = erroredCount;
        this.totalDuration = totalDuration;
        this.alarmingCount = alarmingCount;
        this.connectionAcquisitionCount = connectionAcquisitionCount;
        this.totalConnectionOccupiedTime = totalConnectionOccupiedTime;
        this.alarmingConnectionCount = alarmingConnectionCount;
    }

    public long getCommittedCount() {
        return committedCount;
    }

    public long getRolledBackCount() {
        return rolledBackCount;
    }

    public long getErroredCount() {
        return erroredCount;
    }

    public long getTotalDuration() {
        return totalDuration;
    }

    public long getAlarmingCount() {
        return alarmingCount;
    }

    public long getConnectionAcquisitionCount() {
        return connectionAcquisitionCount;
    }

    public long getAlarmingConnectionCount() {
        return alarmingConnectionCount;
    }

    public long getTotalConnectionOccupiedTime() {
        return totalConnectionOccupiedTime;
    }

    public long getTotalTransaction() {
        return this.committedCount + this.rolledBackCount + this.erroredCount;
    }

    public double getAverageDuration() {
        long totalTransactions = this.getTotalTransaction();
        if (totalTransactions == 0) {
            return 0.0;
        }
        return (double) this.totalDuration / totalTransactions;
    }

    public double getAverageConnectionOccupiedTime() {
        if (this.connectionAcquisitionCount == 0) {
            return 0.0;
        }
        return (double) this.totalConnectionOccupiedTime / this.connectionAcquisitionCount;
    }
}
