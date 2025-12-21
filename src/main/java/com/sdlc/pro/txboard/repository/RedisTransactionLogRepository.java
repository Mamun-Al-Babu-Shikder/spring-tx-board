package com.sdlc.pro.txboard.repository;

import com.sdlc.pro.txboard.domain.PageRequest;
import com.sdlc.pro.txboard.domain.PageResponse;
import com.sdlc.pro.txboard.enums.TransactionPhaseStatus;
import com.sdlc.pro.txboard.model.*;
import com.sdlc.pro.txboard.util.FilterPredicateFactory;
import com.sdlc.pro.txboard.util.SortUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.data.redis.core.RedisTemplate;

import java.util.*;
import java.util.concurrent.ConcurrentSkipListMap;
import java.util.concurrent.atomic.AtomicLong;

public final class RedisTransactionLogRepository implements TransactionLogRepository, InitializingBean {
    private static final Logger log = LoggerFactory.getLogger(RedisTransactionLogRepository.class);

    private final Set<DurationRange> ranges = new LinkedHashSet<>();
    private final int MAX_DURATION_DIST_RANGE;

    private final RedisTemplate<String, TransactionLog> txRedisTemplate;

    private final Map<DurationRange, AtomicLong> durationDistributionMap;
    private final String transactionRedisKey = "tx-board:transaction:";

    public RedisTransactionLogRepository(RedisTemplate<String, TransactionLog> txRedisTemplate, TxBoardProperties txBoardProperties) {
        this.txRedisTemplate = txRedisTemplate;
        this.durationDistributionMap = new ConcurrentSkipListMap<>(Comparator.comparingLong(DurationRange::minMillis));
        List<Integer> buckets = txBoardProperties.getDurationBuckets();
        this.MAX_DURATION_DIST_RANGE = buckets.get(buckets.size() - 1);
        this.buildDurationBucketRange(buckets);
        this.initializeDurationDistributionMap();
    }

    @Override
    public void save(TransactionLog transactionLog) {
        txRedisTemplate.opsForValue().set(transactionRedisKey + transactionLog.getTxId(), transactionLog);
    }

    @Override
    public List<TransactionLog> findAll() {
        return txRedisTemplate.keys(transactionRedisKey + "*").stream()
                .map(txRedisTemplate.opsForValue()::get)
                .toList();
    }

    @Override
    public long count() {
        return txRedisTemplate.keys(transactionRedisKey + "*").size();
    }

    @Override
    public long countByTransactionStatus(TransactionPhaseStatus status) {
        return findAll().stream()
                .filter(txLog -> txLog.getStatus() == status)
                .count();
    }

    @Override
    public double averageDuration() {
        return findAll().stream()
                .mapToDouble(TransactionLog::getDuration)
                .average()
                .orElse(0.0);
    }

    @Override
    public TransactionLogPageResponse findAll(TransactionLogPageRequest pageRequest) {
        List<TransactionLog> logs = pageRequest.getFilter() == FilterNode.UNFILTERED ? findAll() :
                findAll().stream()
                        .filter(FilterPredicateFactory.buildPredicate(pageRequest.getFilter()))
                        .toList();

        List<TransactionLog> sortedLogs = SortUtils.sort(logs, pageRequest.getSort());
        List<TransactionLog> content = getTransactionLogPage(sortedLogs, pageRequest);

        int totalElements = sortedLogs.size();
        return new TransactionLogPageResponse(content, pageRequest, totalElements);
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
    public TransactionSummary getTransactionSummary() {
        List<TransactionLog> allLogs = findAll();
        long committedCount = allLogs.stream().filter(log -> log.getStatus() == TransactionPhaseStatus.COMMITTED).count();
        long rolledBackCount = allLogs.stream().filter(log -> log.getStatus() == TransactionPhaseStatus.ROLLED_BACK).count();
        long erroredCount = allLogs.stream().filter(log -> log.getStatus() == TransactionPhaseStatus.ERRORED).count();

        long totalDuration = allLogs.stream().mapToLong(TransactionLog::getDuration).sum();
        long alarmingCount = allLogs.stream().filter(TransactionLog::isAlarmingTransaction).count();

        long connectionAcquisitionCount = allLogs.stream()
                .mapToLong(log -> log.getConnectionSummary().acquisitionCount())
                .sum();
        long totalConnectionOccupiedTime = allLogs.stream()
                .mapToLong(log -> log.getConnectionSummary().occupiedTime())
                .sum();
        long alarmingConnectionCount = allLogs.stream()
                .mapToLong(log -> log.getConnectionSummary().alarmingConnectionCount())
                .sum();

        return new TransactionSummary(committedCount, rolledBackCount, erroredCount, totalDuration, alarmingCount,
                connectionAcquisitionCount, totalConnectionOccupiedTime, alarmingConnectionCount);
    }

    @Override
    public List<DurationDistribution> getDurationDistributions() {
        for (TransactionLog transactionLog : findAll()) {
            DurationRange range = ranges.stream()
                    .filter(r -> r.matches(transactionLog.getDuration()))
                    .findFirst()
                    .orElse(DurationRange.of(this.MAX_DURATION_DIST_RANGE, Integer.MAX_VALUE));

            this.durationDistributionMap
                    .computeIfAbsent(range, k -> new AtomicLong(0))
                    .incrementAndGet();
        }

        return this.durationDistributionMap.entrySet()
                .stream()
                .map(e -> new DurationDistribution(e.getKey(), e.getValue().get()))
                .toList();
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
    public void deleteAll() {
        assert txRedisTemplate.getConnectionFactory() != null;
        txRedisTemplate.getConnectionFactory().getConnection().serverCommands().flushDb();
    }

    @Override
    public void afterPropertiesSet() throws Exception {
        log.info("The RedisTransactionLogRepository has been created and initialized to support Redis as a storage of transaction logs.");
    }
}
