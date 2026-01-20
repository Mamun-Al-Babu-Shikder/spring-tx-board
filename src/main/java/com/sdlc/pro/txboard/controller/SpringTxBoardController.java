package com.sdlc.pro.txboard.controller;

import com.sdlc.pro.txboard.config.TxBoardProperties;
import com.sdlc.pro.txboard.domain.*;
import com.sdlc.pro.txboard.dto.TransactionChart;
import com.sdlc.pro.txboard.enums.IsolationLevel;
import com.sdlc.pro.txboard.enums.PropagationBehavior;
import com.sdlc.pro.txboard.enums.TransactionPhaseStatus;
import com.sdlc.pro.txboard.model.DurationDistribution;
import com.sdlc.pro.txboard.model.SqlExecutionLog;
import com.sdlc.pro.txboard.model.TransactionLog;
import com.sdlc.pro.txboard.model.TransactionSummary;
import com.sdlc.pro.txboard.repository.SqlExecutionLogRepository;
import com.sdlc.pro.txboard.repository.TransactionLogRepository;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

@RestController
@RequestMapping("/api/tx-board")
public class SpringTxBoardController {

    private final TxBoardProperties txBoardProperties;
    private final TransactionLogRepository transactionLogRepository;
    private final SqlExecutionLogRepository sqlExecutionLogRepository;

    public SpringTxBoardController(TxBoardProperties txBoardProperties,
                                   TransactionLogRepository transactionLogRepository,
                                   SqlExecutionLogRepository sqlExecutionLogRepository) {
        this.txBoardProperties = txBoardProperties;
        this.transactionLogRepository = transactionLogRepository;
        this.sqlExecutionLogRepository = sqlExecutionLogRepository;
    }

    @GetMapping(value = "/config/alarming-threshold", produces = MediaType.APPLICATION_JSON_VALUE)
    public TxBoardProperties.AlarmingThreshold getAlarmingThreshold() {
        return this.txBoardProperties.getAlarmingThreshold();
    }

    @GetMapping(value = "/tx-summary", produces = MediaType.APPLICATION_JSON_VALUE)
    public TransactionSummary getTransactionSummary() {
        return this.transactionLogRepository.getTransactionSummary();
    }

    @GetMapping(value = "/tx-charts", produces = MediaType.APPLICATION_JSON_VALUE)
    public TransactionChart getTransactionChart() {
        List<DurationDistribution> durationDistributions = transactionLogRepository.getDurationDistributions();
        return new TransactionChart(durationDistributions);
    }

    @GetMapping(value = "/tx-logs", produces = MediaType.APPLICATION_JSON_VALUE)
    public PageResponse<TransactionLog> getTransactionLogs(
            @RequestParam(value = "page", defaultValue = "0") int page,
            @RequestParam(value = "size", defaultValue = "10") int size,
            @RequestParam(value = "sort", required = false) String sort,
            @RequestParam(value = "search", required = false) String search,
            @RequestParam(value = "status", required = false) String status,
            @RequestParam(value = "propagation", required = false) String propagation,
            @RequestParam(value = "isolation", required = false) String isolation,
            @RequestParam(value = "connectionOriented", required = false) Boolean connectionOriented) {

        if (page < 0) {
            throw new IllegalArgumentException("The value of 'page' must be positive integer");
        }

        if (size < 1 || size > 1000) {
            throw new IllegalArgumentException("The value of 'size' must be between 1 to 1000");
        }

        FilterNode filter = buildFilter(search, status, propagation, isolation, connectionOriented);
        return transactionLogRepository.findAll(
                PageRequest.of(page, size, parseSort(sort), filter)
        );
    }

    private Sort parseSort(String sort) {
        return sort != null ? Sort.from(sort) : Sort.UNSORTED;
    }

    private static FilterNode buildFilter(String search, String status, String propagation, String isolation, Boolean connectionOriented) {
        List<FilterNode> filters = new ArrayList<>();
        if (search != null && !search.isBlank()) {
            filters.add(FilterGroup.of(
                    List.of(
                            Filter.of("method", search, Filter.Operator.CONTAINS),
                            Filter.of("thread", search, Filter.Operator.CONTAINS)
                    ),
                    FilterGroup.Logic.OR
            ));
        }

        if (status != null && !status.isBlank()) {
            try {
                filters.add(Filter.of("status", TransactionPhaseStatus.valueOf(status), Filter.Operator.EQUALS));
            } catch (Exception e) {
                throw new IllegalArgumentException("The value of status must be " + Arrays.toString(TransactionPhaseStatus.values()));
            }
        }

        if (propagation != null && !propagation.isBlank()) {
            try {
                filters.add(Filter.of("propagation", PropagationBehavior.valueOf(propagation), Filter.Operator.EQUALS));
            } catch (Exception e) {
                throw new IllegalArgumentException("The value of propagation must be " + Arrays.toString(PropagationBehavior.values()));
            }
        }

        if (isolation != null && !isolation.isBlank()) {
            try {
                filters.add(Filter.of("isolation", IsolationLevel.valueOf(isolation), Filter.Operator.EQUALS));
            } catch (Exception e) {
                throw new IllegalArgumentException("The value of isolation must be " + Arrays.toString(IsolationLevel.values()));
            }
        }

        if (connectionOriented != null) {
            filters.add(Filter.of("connectionOriented", connectionOriented, Filter.Operator.EQUALS));
        }

        return filters.isEmpty() ? FilterNode.UNFILTERED : FilterGroup.of(filters, FilterGroup.Logic.AND);
    }

    @GetMapping(value = "/sql-logs", produces = MediaType.APPLICATION_JSON_VALUE)
    public PageResponse<SqlExecutionLog> getTransactionLogs(
            @RequestParam(value = "page", defaultValue = "0") int page,
            @RequestParam(value = "size", defaultValue = "10") int size,
            @RequestParam(value = "sort", required = false) String sort,
            @RequestParam(value = "search", required = false) String search) {

        if (page < 0) {
            throw new IllegalArgumentException("The value of 'page' must be positive integer");
        }

        if (size < 1 || size > 1000) {
            throw new IllegalArgumentException("The value of 'size' must be between 1 to 1000");
        }

        FilterNode filter = search != null && !search.isBlank() ? FilterGroup.of(
                List.of(Filter.of("thread", search, Filter.Operator.CONTAINS)),
                FilterGroup.Logic.OR
        ) : FilterNode.UNFILTERED;

        return this.sqlExecutionLogRepository.findAll(
                PageRequest.of(page, size, parseSort(sort), filter));
    }
}
