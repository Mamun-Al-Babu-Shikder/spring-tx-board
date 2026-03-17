package com.sdlc.pro.txboard.enums;

import java.io.Serializable;
import java.util.Arrays;

public enum IsolationLevel implements Serializable {
    DEFAULT(-1),
    READ_UNCOMMITTED(1),
    READ_COMMITTED(2),
    REPEATABLE_READ(4),
    SERIALIZABLE(8);

    private final int value;

    IsolationLevel(int value) {
        this.value = value;
    }

    public int value() {
        return this.value;
    }

    public static IsolationLevel of(int value) {
        return Arrays.stream(values())
                .filter(p -> p.value() == value)
                .findFirst()
                .orElse(DEFAULT);
    }
}
