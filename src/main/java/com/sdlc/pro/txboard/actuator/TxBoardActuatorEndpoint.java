package com.sdlc.pro.txboard.actuator;

import com.sdlc.pro.txboard.model.DurationDistribution;
import com.sdlc.pro.txboard.model.TransactionSummary;
import com.sdlc.pro.txboard.repository.TransactionLogRepository;
import org.springframework.boot.actuate.endpoint.annotation.Endpoint;
import org.springframework.boot.actuate.endpoint.annotation.ReadOperation;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Endpoint(id = "txboard")
public class TxBoardActuatorEndpoint {
    private final TransactionLogRepository transactionLogRepository;

    public TxBoardActuatorEndpoint(TransactionLogRepository transactionLogRepository) {
        this.transactionLogRepository = transactionLogRepository;
    }

    @ReadOperation
    public Map<String, Object> txBoardInfo() {
        TransactionSummary summary = transactionLogRepository.getTransactionSummary();
        List<DurationDistribution> distributions = transactionLogRepository.getDurationDistributions();

        Map<String, Object> result = new HashMap<>();
        
        // Transaction counts
        Map<String, Object> transactions = new HashMap<>();
        transactions.put("total", summary.getTotalTransaction());
        transactions.put("committed", summary.getCommittedCount());
        transactions.put("rolledBack", summary.getRolledBackCount());
        transactions.put("errored", summary.getErroredCount());
        transactions.put("alarming", summary.getAlarmingCount());
        result.put("transactions", transactions);

        // Duration metrics
        Map<String, Object> duration = new HashMap<>();
        duration.put("total", summary.getTotalDuration());
        duration.put("average", summary.getAverageDuration());
        result.put("duration", duration);

        // Connection metrics
        Map<String, Object> connections = new HashMap<>();
        connections.put("acquisitions", summary.getConnectionAcquisitionCount());
        connections.put("totalOccupiedTime", summary.getTotalConnectionOccupiedTime());
        connections.put("averageOccupiedTime", summary.getAverageConnectionOccupiedTime());
        connections.put("alarming", summary.getAlarmingConnectionCount());
        result.put("connections", connections);

        // Duration distribution
        result.put("durationDistribution", distributions);

        return result;
    }
}
