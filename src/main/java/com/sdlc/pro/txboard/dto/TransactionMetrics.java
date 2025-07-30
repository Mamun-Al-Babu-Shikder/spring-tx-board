package com.sdlc.pro.txboard.dto;

public record TransactionMetrics(long totalTransactions,
                                 long committedCount,
                                 long rolledBackCount,
                                 double successRate,
                                 double avgDuration) {
}
