package com.sdlc.pro.txboard.dto;

public class TransactionMetrics {
    private final long totalTransactions;
    private final long committedCount;
    private final long rolledBackCount;
    private final double successRate;
    private final double avgDuration;

    public TransactionMetrics(long totalTransactions,
                              long committedCount,
                              long rolledBackCount,
                              double successRate,
                              double avgDuration) {
        this.totalTransactions = totalTransactions;
        this.committedCount = committedCount;
        this.rolledBackCount = rolledBackCount;
        this.successRate = successRate;
        this.avgDuration = avgDuration;
    }

    public long getTotalTransactions() { return totalTransactions; }
    public long getCommittedCount() { return committedCount; }
    public long getRolledBackCount() { return rolledBackCount; }
    public double getSuccessRate() { return successRate; }
    public double getAvgDuration() { return avgDuration; }
}
