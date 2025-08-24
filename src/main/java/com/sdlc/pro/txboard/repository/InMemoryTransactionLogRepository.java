package com.sdlc.pro.txboard.repository;

import com.sdlc.pro.txboard.config.TxBoardProperties;
import com.sdlc.pro.txboard.domain.FilterNode;
import com.sdlc.pro.txboard.domain.TransactionLogPageRequest;
import com.sdlc.pro.txboard.domain.TransactionLogPageResponse;
import com.sdlc.pro.txboard.enums.TransactionPhaseStatus;
import com.sdlc.pro.txboard.model.DurationDistribution;
import com.sdlc.pro.txboard.model.DurationRange;
import com.sdlc.pro.txboard.model.TransactionLog;
import com.sdlc.pro.txboard.util.FilterPredicateFactory;
import com.sdlc.pro.txboard.util.SortUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.InitializingBean;

import java.util.*;
import java.util.concurrent.ConcurrentSkipListMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicLong;

import static java.util.Collections.emptyList;
import static java.util.stream.Collectors.toList;

public final class InMemoryTransactionLogRepository implements TransactionLogRepository, InitializingBean {
    private static final Logger log = LoggerFactory.getLogger(InMemoryTransactionLogRepository.class);

    private final Set<DurationRange> ranges = new LinkedHashSet<>();
    private final int MAX_DURATION_DIST_RANGE;

    private final List<TransactionLog> transactionLogs;
    private final Map<DurationRange, AtomicLong> durationDistributionMap;

    public InMemoryTransactionLogRepository(TxBoardProperties txBoardProperties) {
        this.transactionLogs = new CopyOnWriteArrayList<>();
        this.durationDistributionMap = new ConcurrentSkipListMap<>(Comparator.comparingLong(DurationRange::getMinMillis));
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
        return new ArrayList<TransactionLog>(transactionLogs);
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
                        .collect(toList());

        List<TransactionLog> sortedLogs = SortUtils.sort(logs, pageRequest.getSort());
        List<TransactionLog> content = getTransactionLogPage(sortedLogs, pageRequest);

        int totalElements = sortedLogs.size();
        return new TransactionLogPageResponse(content, pageRequest, totalElements);
    }

    @Override
    public List<DurationDistribution> getDurationDistributions() {
        return this.durationDistributionMap.entrySet()
                .stream()
                .map(e -> new DurationDistribution(e.getKey(), e.getValue().get()))
                .collect(toList());
    }


    private static List<TransactionLog> getTransactionLogPage(List<TransactionLog> logs, TransactionLogPageRequest pageRequest) {
        try {
            int start = pageRequest.getPageNumber() * pageRequest.getPageSize();
            int end = (pageRequest.getPageNumber() + 1) * pageRequest.getPageSize();
            return logs.subList(start, Math.min(end, logs.size()));
        } catch (IndexOutOfBoundsException | IllegalArgumentException ex) {
            return emptyList();
        }
    }

    @Override
    public void afterPropertiesSet() throws Exception {
        log.info("The InMemoryTransactionLogRepository has been created and initialized to support in-memory storage of transaction logs.");
    }
}
