package com.sdlc.pro.txboard.dto;

import com.sdlc.pro.txboard.model.DurationDistribution;

import java.util.List;

public class TransactionChart {
    private final List<DurationDistribution> durationDistribution;

    public TransactionChart(List<DurationDistribution> durationDistribution) {
        this.durationDistribution = durationDistribution;
    }

    public List<DurationDistribution> getDurationDistribution() { return durationDistribution; }
}
