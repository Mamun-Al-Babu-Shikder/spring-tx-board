package com.sdlc.pro.txboard.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.sdlc.pro.txboard.enums.IsolationLevel;
import com.sdlc.pro.txboard.enums.PropagationBehavior;
import com.sdlc.pro.txboard.enums.TransactionPhaseStatus;

import java.io.Serializable;
import java.time.Duration;
import java.time.Instant;
import java.util.List;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class TransactionLog implements Serializable {
    private final Integer txId;
    private final String method;
    private final PropagationBehavior propagation;
    private final IsolationLevel isolation;
    private final Instant startTime;
    private final Instant endTime;
    private final long duration;
    private final int connectionAcquisitionCount;
    private final long connectionOccupiedTime;
    private final long alarmingThreshold;
    private final TransactionPhaseStatus status;
    private final String thread;
    private final List<TransactionLog> child;
    private final List<String> executedQuires;
    private final List<TransactionEvent> events;

    public TransactionLog(Integer txId, String method, PropagationBehavior propagation, IsolationLevel isolation,
                          Instant startTime, Instant endTime, int connectionAcquisitionCount,
                          long connectionOccupiedTime, long alarmingThreshold, TransactionPhaseStatus status,
                          String thread, List<TransactionLog> child, List<String> executedQuires,
                          List<TransactionEvent> events) {
        this.txId = txId;
        this.method = method;
        this.propagation = propagation;
        this.isolation = isolation;
        this.startTime = startTime;
        this.endTime = endTime;
        this.status = status;
        this.duration = Duration.between(startTime, endTime).toMillis();
        this.connectionAcquisitionCount = connectionAcquisitionCount;
        this.connectionOccupiedTime = connectionOccupiedTime;
        this.alarmingThreshold = alarmingThreshold;
        this.thread = thread;
        this.child = child;
        this.executedQuires = executedQuires;
        this.events = events;
    }

    public Integer getTxId() {
        return txId;
    }

    public String getMethod() {
        return method;
    }

    public PropagationBehavior getPropagation() {
        return propagation;
    }

    public IsolationLevel getIsolation() {
        return isolation;
    }

    public long getAlarmingThreshold() {
        return alarmingThreshold;
    }

    public Instant getStartTime() {
        return startTime;
    }

    public Instant getEndTime() {
        return endTime;
    }

    public TransactionPhaseStatus getStatus() {
        return status;
    }

    public String getThread() {
        return thread;
    }

    public long getDuration() {
        return duration;
    }

    public int getConnectionAcquisitionCount() {
        return connectionAcquisitionCount;
    }

    public long getConnectionOccupiedTime() {
        return connectionOccupiedTime;
    }

    public boolean isAlarming() {
        return this.connectionOccupiedTime > this.alarmingThreshold;
    }

    public List<TransactionLog> getChild() {
        return this.child == null ? List.of() : this.child;
    }

    public List<String> getExecutedQuires() {
        return this.executedQuires == null ? List.of() : this.executedQuires;
    }

    public List<TransactionEvent> getEvents() {
        return events;
    }

    public int getTotalTransactionCount() {
        return 1 + getChild().stream()
                .mapToInt(TransactionLog::getTotalTransactionCount)
                .sum();
    }

    public int getTotalQueryCount() {
        return this.getExecutedQuires().size() + getChild().stream()
                .mapToInt(TransactionLog::getTotalQueryCount)
                .sum();
    }
}
