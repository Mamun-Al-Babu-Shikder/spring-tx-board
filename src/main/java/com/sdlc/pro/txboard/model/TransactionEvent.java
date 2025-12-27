package com.sdlc.pro.txboard.model;

import java.io.Serializable;
import java.time.Instant;
import java.util.Objects;

public class TransactionEvent implements Serializable {
    private Type type;
    private Instant timestamp;
    private String details;

    public TransactionEvent() {}

    public TransactionEvent(Type type, String details) {
        this(type, Instant.now(), details);
    }

    public TransactionEvent(Type type, Instant timestamp, String details) {
        this.type = Objects.requireNonNull(type, "Must be valid transaction event type");
        this.timestamp = Objects.requireNonNull(timestamp, "Must be valid event timestamp");
        this.details = details;
    }

    public Type getType() {
        return type;
    }

    public void setType(Type type) {
        this.type = type;
    }

    public Instant getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(Instant timestamp) {
        this.timestamp = timestamp;
    }

    public String getDetails() {
        return details;
    }

    public void setDetails(String details) {
        this.details = details;
    }

    public enum Type {
        TRANSACTION_START,
        TRANSACTION_END,
        CONNECTION_ACQUIRED,
        CONNECTION_RELEASED,
    }
}

