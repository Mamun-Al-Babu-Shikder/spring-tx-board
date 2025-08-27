package com.sdlc.pro.txboard.repository;

import com.sdlc.pro.txboard.config.TxBoardProperties;
import com.sdlc.pro.txboard.domain.FilterNode;
import com.sdlc.pro.txboard.domain.TransactionLogPageRequest;
import com.sdlc.pro.txboard.domain.TransactionLogPageResponse;
import com.sdlc.pro.txboard.enums.TransactionPhaseStatus;
import com.sdlc.pro.txboard.model.*;
import com.sdlc.pro.txboard.util.FilterPredicateFactory;
import com.sdlc.pro.txboard.util.SortUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.InitializingBean;

import java.util.*;
import java.util.concurrent.ConcurrentSkipListMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;

public final class InMemoryTransactionLogRepository implements TransactionLogRepository, InitializingBean {
    private static final Logger log = LoggerFactory.getLogger(InMemoryTransactionLogRepository.class);

    private final Set<DurationRange> ranges = new LinkedHashSet<>();
    private final int MAX_DURATION_DIST_RANGE;

    private final List<TransactionLog> transactionLogs;
    private final AtomicReference<TransactionSummary> summaryAtomicReference;
    private final Map<DurationRange, AtomicLong> durationDistributionMap;

    public InMemoryTransactionLogRepository(TxBoardProperties txBoardProperties) {
        this.transactionLogs = new CopyOnWriteArrayList<>();
        this.summaryAtomicReference = new AtomicReference<>(new TransactionSummary(0, 0, 0, 0, 0, 0, 0, 0));
        this.durationDistributionMap = new ConcurrentSkipListMap<>(Comparator.comparingLong(DurationRange::minMillis));
        List<Integer> buckets = txBoardProperties.getDurationBuckets();
        this.MAX_DURATION_DIST_RANGE = buckets.get(buckets.size() - 1);
        this.buildDurationBucketRange(buckets);
        this.initializeDurationDistributionMap();
    }

    private void buildDurationBucketRange(List<Integer> buckets) {
        int prev = 0;
        for (int curr : buckets) {
            ranges.add(DurationRange.of(prev, curr));
            prev = curr;
        }
    }

    private void initializeDurationDistributionMap() {
        for (DurationRange range : ranges) {
            this.durationDistributionMap.put(range, new AtomicLong(0));
        }
    }

    @Override
    public void save(TransactionLog transactionLog) {
        this.transactionLogs.add(transactionLog);
        this.updateDurationDistribution(transactionLog);
        this.updateTransactionSummary(transactionLog);
    }

    private void updateTransactionSummary(TransactionLog transactionLog) {
        this.summaryAtomicReference.updateAndGet(summary -> {
            long committedCount = summary.getCommittedCount();
            long rolledBackCount = summary.getRolledBackCount();
            long erroredCount = summary.getErroredCount();

            switch (transactionLog.getStatus()) {
                case COMMITTED -> committedCount++;
                case ROLLED_BACK -> rolledBackCount++;
                case ERRORED -> erroredCount++;
            }

            long totalDuration = summary.getTotalDuration() + transactionLog.getDuration();
            long alarmingCount = summary.getAlarmingCount() + (transactionLog.isAlarmingTransaction() ? 1 : 0);

            ConnectionSummary connectionSummary = transactionLog.getConnectionSummary();

            long connectionAcquisitionCount = summary.getConnectionAcquisitionCount() + connectionSummary.acquisitionCount();
            long totalConnectionOccupiedTime = summary.getTotalConnectionOccupiedTime() + connectionSummary.occupiedTime();
            long alarmingConnectionCount = summary.getAlarmingConnectionCount() + connectionSummary.alarmingConnectionCount();

            return new TransactionSummary(committedCount, rolledBackCount, erroredCount, totalDuration, alarmingCount,
                    connectionAcquisitionCount, totalConnectionOccupiedTime, alarmingConnectionCount);
        });
    }

    private void updateDurationDistribution(TransactionLog transactionLog) {
        DurationRange range = ranges.stream()
                .filter(r -> r.matches(transactionLog.getDuration()))
                .findFirst()
                .orElse(DurationRange.of(this.MAX_DURATION_DIST_RANGE, Integer.MAX_VALUE));

        this.durationDistributionMap
                .computeIfAbsent(range, k -> new AtomicLong(0))
                .incrementAndGet();
    }

    public List<TransactionLog> findAll() {
        return List.copyOf(transactionLogs);
    }

    @Override
    public long count() {
        return transactionLogs.size();
    }

    @Override
    public long countByTransactionStatus(TransactionPhaseStatus status) {
        return this.transactionLogs.stream()
                .filter(t -> t.getStatus() == status)
                .count();
    }

    @Override
    public double averageDuration() {
        return transactionLogs.stream()
                .mapToDouble(TransactionLog::getDuration)
                .average().orElse(0.0);
    }

    @Override
    public TransactionLogPageResponse findAll(TransactionLogPageRequest pageRequest) {
        List<TransactionLog> logs = pageRequest.getFilter() == FilterNode.UNFILTERED ? this.transactionLogs :
                this.transactionLogs.stream()
                        .filter(FilterPredicateFactory.buildPredicate(pageRequest.getFilter()))
                        .toList();

        List<TransactionLog> sortedLogs = SortUtils.sort(logs, pageRequest.getSort());
        List<TransactionLog> content = getTransactionLogPage(sortedLogs, pageRequest);

        int totalElements = sortedLogs.size();
        return new TransactionLogPageResponse(content, pageRequest, totalElements);
    }

    @Override
    public TransactionSummary getTransactionSummary() {
        return this.summaryAtomicReference.get();
    }

    @Override
    public List<DurationDistribution> getDurationDistributions() {
        return this.durationDistributionMap.entrySet()
                .stream()
                .map(e -> new DurationDistribution(e.getKey(), e.getValue().get()))
                .toList();
    }


    private static List<TransactionLog> getTransactionLogPage(List<TransactionLog> logs, TransactionLogPageRequest pageRequest) {
        try {
            int start = pageRequest.getPageNumber() * pageRequest.getPageSize();
            int end = (pageRequest.getPageNumber() + 1) * pageRequest.getPageSize();
            return logs.subList(start, Math.min(end, logs.size()));
        } catch (IndexOutOfBoundsException | IllegalArgumentException ex) {
            return List.of();
        }
    }

    @Override
    public void afterPropertiesSet() throws Exception {
        log.info("The InMemoryTransactionLogRepository has been created and initialized to support in-memory storage of transaction logs.");
    }
}
