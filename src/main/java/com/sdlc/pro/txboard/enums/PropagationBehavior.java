package com.sdlc.pro.txboard.enums;

import java.util.Arrays;

public enum PropagationBehavior {
    UNKNOWN(-1),
    REQUIRED(0),
    SUPPORTS(1),
    MANDATORY(2),
    REQUIRES_NEW(3),
    NOT_SUPPORTED(4),
    NEVER(5),
    NESTED(6);

    private final int value;

    PropagationBehavior(int value) {
        this.value = value;
    }

    public int value() {
        return this.value;
    }

    public static PropagationBehavior of(int value) {
        return Arrays.stream(values())
                .filter(p -> p.value() == value)
                .findFirst()
                .orElse(UNKNOWN);
    }
}
