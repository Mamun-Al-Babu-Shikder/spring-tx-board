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
    private final ConnectionSummary connectionSummary;
    private final TransactionPhaseStatus status;
    private final String thread;
    private final List<String> executedQuires;
    private final List<TransactionLog> child;
    private final List<TransactionEvent> events;
    private final boolean alarmingTransaction;
    private final Boolean havingAlarmingConnection;

    public TransactionLog(Integer txId, String method, PropagationBehavior propagation, IsolationLevel isolation,
                          Instant startTime, Instant endTime, ConnectionSummary connectionSummary,
                          TransactionPhaseStatus status, String thread, List<String> executedQuires,
                          List<TransactionLog> child, List<TransactionEvent> events, long txAlarmingThreshold,
                          long conAlarmingThreshold) {
        this.txId = txId;
        this.method = method;
        this.propagation = propagation;
        this.isolation = isolation;
        this.startTime = startTime;
        this.endTime = endTime;
        this.status = status;
        this.duration = Duration.between(startTime, endTime).toMillis();
        this.connectionSummary = connectionSummary;
        this.thread = thread;
        this.child = child;
        this.executedQuires = executedQuires;
        this.events = events;
        this.alarmingTransaction = this.duration > txAlarmingThreshold;
        this.havingAlarmingConnection = this.connectionSummary != null ?
                this.connectionSummary.occupiedTime() > conAlarmingThreshold : null;
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

    public ConnectionSummary getConnectionSummary() {
        return this.connectionSummary;
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

    public boolean isAlarmingTransaction() {
        return this.alarmingTransaction;
    }

    public Boolean getHavingAlarmingConnection() {
        return havingAlarmingConnection;
    }

    public boolean isHealthyTransaction() {
        if (this.havingAlarmingConnection == null) {
            return !this.alarmingTransaction;
        }

        return !this.alarmingTransaction && !this.havingAlarmingConnection;
    }
}
