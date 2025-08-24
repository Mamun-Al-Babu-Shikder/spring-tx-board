package com.sdlc.pro.txboard.model;

public class DurationRange {
    private final long minMillis;
    private final long maxMillis;

    public DurationRange(long minMillis, long maxMillis) {
        this.minMillis = minMillis;
        this.maxMillis = maxMillis;
    }

    public static DurationRange of(long minMillis, long maxMillis) {
        return new DurationRange(minMillis, maxMillis);
    }

    /**
     * minMillis inclusive
     * maxMillis exclusive
     */
    public boolean matches(long millis) {
        return millis >= minMillis && millis < maxMillis;
    }

    public long getMinMillis() { return minMillis; }
    public long getMaxMillis() { return maxMillis; }
}
