package com.sdlc.pro.txboard.model;

public record DurationRange(long minMillis, long maxMillis) {
    public static DurationRange of(long minMillis, long maxMillis) {
        return new DurationRange(minMillis,  maxMillis);
    }

    /**
     * minMillis inclusive
     * maxMillis exclusive
     */
    public boolean matches(long millis) {
        return millis >= minMillis && millis < maxMillis;
    }
}
