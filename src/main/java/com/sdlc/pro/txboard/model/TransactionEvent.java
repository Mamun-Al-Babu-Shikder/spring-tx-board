package com.sdlc.pro.txboard.model;

import com.fasterxml.jackson.annotation.JsonCreator;

import java.time.Instant;
import java.util.Objects;

public class TransactionEvent {
    private final Type type;
    private final Instant timestamp;
    private final String details;

    public TransactionEvent(Type type, String details) {
        this(type, Instant.now(), details);
    }

    public TransactionEvent(Type type, Instant timestamp, String details) {
        this.type = Objects.requireNonNull(type, "Must be valid transaction event type");
        this.timestamp = Objects.requireNonNull(timestamp, "Must be valid event timestamp");
        this.details = details;
    }

    @JsonCreator
    public TransactionEvent() {
        this.type = null;
        this.timestamp = null;
        this.details = null;
    }

    public Type getType() {
        return type;
    }

    public Instant getTimestamp() {
        return timestamp;
    }

    public String getDetails() {
        return details;
    }

    public enum Type {
        TRANSACTION_START,
        TRANSACTION_END,
        CONNECTION_ACQUIRED,
        CONNECTION_RELEASED,
    }
}

