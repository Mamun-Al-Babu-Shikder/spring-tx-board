package com.sdlc.pro.txboard.dto;

import com.sdlc.pro.txboard.model.DurationDistribution;

import java.util.List;

public record TransactionChart(List<DurationDistribution> durationDistribution) {
}
