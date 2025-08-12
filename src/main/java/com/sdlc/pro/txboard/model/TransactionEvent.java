package com.sdlc.pro.txboard.model;

import java.time.Instant;
import java.util.Objects;

public class TransactionEvent {
    private final Type type;
    private final Instant timestamp;
    private final String details;

    public TransactionEvent(Type type, String details) {
        this.type = Objects.requireNonNull(type, "Must be valid transaction event type");
        this.timestamp = Instant.now();
        this.details = details;
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

