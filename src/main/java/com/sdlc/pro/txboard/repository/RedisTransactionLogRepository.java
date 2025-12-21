
package com.sdlc.pro.txboard.repository;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.redis.om.spring.search.stream.EntityStream;
import com.sdlc.pro.txboard.config.TxBoardProperties;
import com.sdlc.pro.txboard.domain.*;
import com.sdlc.pro.txboard.enums.TransactionPhaseStatus;
import com.sdlc.pro.txboard.model.*;

import com.sdlc.pro.txboard.model.TransactionLogDocument;
import com.sdlc.pro.txboard.util.RedisFactory;
import com.sdlc.pro.txboard.util.TransactionLogMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.data.redis.core.StringRedisTemplate;

import java.lang.reflect.Type;
import java.util.*;
import java.util.concurrent.ConcurrentSkipListMap;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.StreamSupport;

public final class RedisTransactionLogRepository implements TransactionLogRepository, InitializingBean {
    private static final Logger log = LoggerFactory.getLogger(RedisTransactionLogRepository.class);

    private final StringRedisTemplate redisTemplate;
    private final TxRedisDocumentRepository redisRepository;
    private final EntityStream entityStream;
    private final Gson gson;
    private final String TX_SUMMARY_KEY = "txboard:summary";
    private final String TX_DURATION_DISTRIBUTION_KEY = "txboard:duration-distribution";

    private final int MAX_DURATION_DIST_RANGE;
    private final Set<DurationRange> ranges = new LinkedHashSet<>();

    public RedisTransactionLogRepository(TxBoardProperties txBoardProperties, StringRedisTemplate redisTemplate, TxRedisDocumentRepository redisRepository, EntityStream entityStream, Gson gson) {
        List<Integer> buckets = txBoardProperties.getDurationBuckets();
        this.MAX_DURATION_DIST_RANGE = buckets.get(buckets.size() - 1);

        this.redisTemplate = redisTemplate;
        this.redisRepository = redisRepository;
        this.entityStream = entityStream;
        this.gson = gson;

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
        String durationDistributionObject = redisTemplate.opsForValue().get(TX_DURATION_DISTRIBUTION_KEY);
        if(durationDistributionObject == null){
            Map<String, AtomicLong> durationDistributionMap = new ConcurrentSkipListMap<>(Comparator.comparingLong(key -> Long.parseLong(key.split("-")[0])));
            for (DurationRange range : ranges) {
                durationDistributionMap.put(range.minMillis() + "-" + range.maxMillis(), new AtomicLong(0));
            }
            redisTemplate.opsForValue().set(TX_DURATION_DISTRIBUTION_KEY, gson.toJson(durationDistributionMap));
        }
    }

    @Override
    public void save(TransactionLog log){
        TransactionLogDocument document = TransactionLogMapper.toDocument(log);
        redisRepository.save(document);
        this.updateTransactionSummary(log);
        this.updateDurationDistribution(log);
    }

    @Override
    public long count(){
        return redisRepository.count();
    }

    @Override
    public long countByTransactionStatus(TransactionPhaseStatus status) {
        List<TransactionLogDocument> docs = redisRepository.searchByStatus(status.name());
        return docs.size();
    }

    @Override
    public double averageDuration() {
        try (var stream = entityStream.of(TransactionLogDocument.class)) {
            return (long) stream
                    .mapToLong(TransactionLogDocument::getDuration)
                    .average()
                    .orElse(0);
        }
    }

    @Override
    public List<TransactionLog> findAll(){
        return redisRepository.findAll().stream()
                .map(TransactionLogMapper::fromDocument)
                .toList();
    }

    @Override
    public PageResponse<TransactionLog> findAll(PageRequest request) {
        int page = request.getPageNumber();
        int size = request.getPageSize();
        long total;

        var stream = entityStream.of(TransactionLogDocument.class);

        if (!request.getFilter().equals(FilterNode.UNFILTERED)) {
            var predicate = RedisFactory.predicate(request.getFilter());

            stream = stream
                    .filter(predicate);

            total = stream.count();
        }else{
            total = redisRepository.count();
        }

        if (!request.getSort().equals(Sort.UNSORTED)) {
            var property = RedisFactory.sortProperty(request.getSort());
            var direction = RedisFactory.sortDirection(request.getSort());
            stream = stream.sorted(property, direction);
        }

        stream = stream
                .skip((long) page * size)
                .limit(size);

        List<TransactionLog> content = StreamSupport
                .stream(stream.spliterator(), false)
                .map(TransactionLogMapper::fromDocument)
                .toList();

        return new PageResponse<>(content, request, total);
    }

    @Override
    public TransactionSummary getTransactionSummary(){
        String summaryObject = redisTemplate.opsForValue().get(TX_SUMMARY_KEY);
        return gson.fromJson(summaryObject, TransactionSummary.class);
    }

    private void updateTransactionSummary(TransactionLog transactionLog) {
        String summaryString = redisTemplate.opsForValue().get(TX_SUMMARY_KEY);
        if(summaryString != null){
            TransactionSummary summary = gson.fromJson(summaryString, TransactionSummary.class);
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

            TransactionSummary newSummary = new TransactionSummary(committedCount, rolledBackCount, erroredCount, totalDuration, alarmingCount,
                    connectionAcquisitionCount, totalConnectionOccupiedTime, alarmingConnectionCount);
            redisTemplate.opsForValue().set("txboard:summary", gson.toJson(newSummary));
        }else{
            TransactionSummary summary = new TransactionSummary(0, 0, 0, 0, 0, 0, 0, 0);
            redisTemplate.opsForValue().set("txboard:summary", gson.toJson(summary));
        }
    }

    @Override
    public List<DurationDistribution> getDurationDistributions() {
        String durationDistributionObject =
                redisTemplate.opsForValue().get(TX_DURATION_DISTRIBUTION_KEY);

        Type type = new TypeToken<Map<String, AtomicLong>>() {}.getType();
        Map<String, AtomicLong> durationDistributionMap =
                gson.fromJson(durationDistributionObject, type);

        return durationDistributionMap.entrySet()
                .stream()
                .map(e -> new DurationDistribution(
                        durationRangeParse(e.getKey()),
                        e.getValue().get()
                ))
                .toList();
    }

    private static DurationRange durationRangeParse(String value) {
        String[] parts = value.split("-");
        if (parts.length != 2) {
            throw new IllegalArgumentException("Invalid DurationRange: " + value);
        }
        return new DurationRange(
                Long.parseLong(parts[0]),
                Long.parseLong(parts[1])
        );
    }

    private void updateDurationDistribution(TransactionLog transactionLog) {
        DurationRange range = ranges.stream()
                .filter(r -> r.matches(transactionLog.getDuration()))
                .findFirst()
                .orElse(DurationRange.of(this.MAX_DURATION_DIST_RANGE, Integer.MAX_VALUE));

        String durationDistributionObject = redisTemplate.opsForValue().get(TX_DURATION_DISTRIBUTION_KEY);
        if(durationDistributionObject == null){
            this.initializeDurationDistributionMap();
            durationDistributionObject = redisTemplate.opsForValue().get(TX_DURATION_DISTRIBUTION_KEY);
        }

        Type type = new TypeToken<Map<String, AtomicLong>>() {}.getType();

        Map<String, AtomicLong> durationDistributionMap = gson.fromJson(durationDistributionObject, type);

        durationDistributionMap
                .computeIfAbsent(range.minMillis() + "-" + range.maxMillis(), k -> new AtomicLong(0))
                .incrementAndGet();

        redisTemplate.opsForValue().set(TX_DURATION_DISTRIBUTION_KEY, gson.toJson(durationDistributionMap));
    }

    public void deleteAll(){
        redisRepository.deleteAll();
    }

    public void afterPropertiesSet() throws Exception {
        log.info("The RedisTransactionLogRepository has been created and initialized to support in-memory storage of transaction logs.");
    }
}

