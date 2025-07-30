package com.sdlc.pro.txboard.model;

import com.sdlc.pro.txboard.enums.TransactionStatus;

import java.io.Serializable;
import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.atomic.AtomicInteger;

public class TransactionLog implements Serializable {
    private static final AtomicInteger ATOMIC_TX_ID_GEN = new AtomicInteger(0);

    private final int txId;
    private final String method;
    private final Instant startTime;
    private final Instant endTime;
    private final long duration;
    private final long alarmingThreshold;
    private final TransactionStatus status;
    private final String thread;

    public TransactionLog(int txId, String method, Instant startTime, Instant endTime, TransactionStatus status, String thread, long alarmingThreshold) {
        this.txId = txId;
        this.method = method;
        this.startTime = startTime;
        this.endTime = endTime;
        this.status = status;
        this.duration = Duration.between(startTime, endTime).toMillis();
        this.thread = thread;
        this.alarmingThreshold = alarmingThreshold;
    }

    public static TransactionLog from(String method, Instant startTime, Instant endTime, TransactionStatus status, String thread, long alarmingThreshold) {
        return new TransactionLog(ATOMIC_TX_ID_GEN.incrementAndGet(), method, startTime, endTime, status, thread, alarmingThreshold);
    }

    public int getTxId() {
        return txId;
    }

    public String getMethod() {
        return method;
    }

    public Instant getStartTime() {
        return startTime;
    }

    public Instant getEndTime() {
        return endTime;
    }

    public TransactionStatus getStatus() {
        return status;
    }

    public String getThread() {
        return thread;
    }

    public long getDuration() {
        return duration;
    }

    public boolean isAlarming() {
        return this.duration > this.alarmingThreshold;
    }
}
