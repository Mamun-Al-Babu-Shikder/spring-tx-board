package com.sdlc.pro.txboard.model;

public class DurationDistribution {
    private final DurationRange range;
    private final long count;

    public DurationDistribution(DurationRange range, long count) {
        this.range = range;
        this.count = count;
    }

    public DurationRange getRange() { return range; }
    public long getCount() { return count; }
}
