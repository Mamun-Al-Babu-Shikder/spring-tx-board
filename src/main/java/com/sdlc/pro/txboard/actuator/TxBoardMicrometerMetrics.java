package com.sdlc.pro.txboard.actuator;

import org.springframework.beans.factory.InitializingBean;

import com.sdlc.pro.txboard.model.TransactionSummary;
import com.sdlc.pro.txboard.repository.TransactionLogRepository;

import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;

public class TxBoardMicrometerMetrics implements InitializingBean {
    private final MeterRegistry meterRegistry;
    private final TransactionLogRepository transactionLogRepository;

    public TxBoardMicrometerMetrics(MeterRegistry meterRegistry, TransactionLogRepository transactionLogRepository) {
        this.meterRegistry = meterRegistry;
        this.transactionLogRepository = transactionLogRepository;
    }

    @Override
    public void afterPropertiesSet() {
        // Transaction count metrics
        Gauge.builder("txboard.transactions.total", () -> getTransactionSummary().getTotalTransaction())
                .description("Total number of transactions processed")
                .register(meterRegistry);

        Gauge.builder("txboard.transactions.committed", () -> getTransactionSummary().getCommittedCount())
                .description("Number of committed transactions")
                .register(meterRegistry);

        Gauge.builder("txboard.transactions.rolled_back", () -> getTransactionSummary().getRolledBackCount())
                .description("Number of rolled back transactions")
                .register(meterRegistry);

        Gauge.builder("txboard.transactions.errored", () -> getTransactionSummary().getErroredCount())
                .description("Number of errored transactions")
                .register(meterRegistry);

        Gauge.builder("txboard.transactions.alarming", () -> getTransactionSummary().getAlarmingCount())
                .description("Number of alarming transactions (slow)")
                .register(meterRegistry);

        // Duration metrics
        Gauge.builder("txboard.transactions.duration.total", () -> getTransactionSummary().getTotalDuration())
                .description("Total duration of all transactions in milliseconds")
                .register(meterRegistry);

        Gauge.builder("txboard.transactions.duration.average", () -> getTransactionSummary().getAverageDuration())
                .description("Average transaction duration in milliseconds")
                .register(meterRegistry);

        // Connection metrics
        Gauge.builder("txboard.connections.acquisitions", () -> getTransactionSummary().getConnectionAcquisitionCount())
                .description("Total number of connection acquisitions")
                .register(meterRegistry);

        Gauge.builder("txboard.connections.occupied_time.total", () -> getTransactionSummary().getTotalConnectionOccupiedTime())
                .description("Total connection occupied time in milliseconds")
                .register(meterRegistry);

        Gauge.builder("txboard.connections.occupied_time.average", () -> getTransactionSummary().getAverageConnectionOccupiedTime())
                .description("Average connection occupied time in milliseconds")
                .register(meterRegistry);

        Gauge.builder("txboard.connections.alarming", () -> getTransactionSummary().getAlarmingConnectionCount())
                .description("Number of alarming connections (long-held)")
                .register(meterRegistry);
    }

    private TransactionSummary getTransactionSummary() {
        return transactionLogRepository.getTransactionSummary();
    }
}
