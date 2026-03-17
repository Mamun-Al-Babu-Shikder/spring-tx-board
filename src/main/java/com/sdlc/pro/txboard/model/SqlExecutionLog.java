package com.sdlc.pro.txboard.model;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public class SqlExecutionLog {
    private final UUID id;
    private final Instant conAcquiredTime;
    private final Instant conReleaseTime;
    private final long conOccupiedTime;
    private final boolean alarmingConnection;
    private final String thread;
    private final List<String> executedQuires;

    public SqlExecutionLog(UUID id, Instant conAcquiredTime, Instant conReleaseTime, boolean alarmingConnection,
                           String thread, List<String> executedQuires) {
        this.id = id;
        this.conAcquiredTime = conAcquiredTime;
        this.conReleaseTime = conReleaseTime;
        this.conOccupiedTime = Duration.between(conAcquiredTime, conReleaseTime).toMillis();
        this.alarmingConnection = alarmingConnection;
        this.thread = thread;
        this.executedQuires = executedQuires;
    }

    public UUID getId() {
        return id;
    }

    public Instant getConAcquiredTime() {
        return conAcquiredTime;
    }

    public Instant getConReleaseTime() {
        return conReleaseTime;
    }

    public long getConOccupiedTime() {
        return conOccupiedTime;
    }

    public boolean isAlarmingConnection() {
        return alarmingConnection;
    }

    public String getThread() {
        return thread;
    }

    public List<String> getExecutedQuires() {
        return executedQuires;
    }
}
