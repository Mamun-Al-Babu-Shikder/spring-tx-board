package com.sdlc.pro.txboard.model;

import java.time.Duration;
import java.time.Instant;
import java.util.List;

public class SqlExecutionLog {
    private final Instant conAcquiredTime;
    private final Instant conReleaseTime;
    private final long conOccupiedTime;
    private final String thread;
    private final List<String> executedQuires;

    public SqlExecutionLog(Instant conAcquiredTime, Instant conReleaseTime, String thread, List<String> executedQuires) {
        this.conAcquiredTime = conAcquiredTime;
        this.conReleaseTime = conReleaseTime;
        this.conOccupiedTime = Duration.between(conAcquiredTime, conReleaseTime).toMillis();
        this.thread = thread;
        this.executedQuires = executedQuires;
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

    public String getThread() {
        return thread;
    }

    public List<String> getExecutedQuires() {
        return executedQuires;
    }
}
