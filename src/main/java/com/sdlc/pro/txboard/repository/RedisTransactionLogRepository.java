package com.sdlc.pro.txboard.repository;

import com.sdlc.pro.txboard.config.TxBoardProperties;
import com.sdlc.pro.txboard.domain.PageRequest;
import com.sdlc.pro.txboard.domain.PageResponse;
import com.sdlc.pro.txboard.enums.IsolationLevel;
import com.sdlc.pro.txboard.enums.PropagationBehavior;
import com.sdlc.pro.txboard.enums.TransactionPhaseStatus;
import com.sdlc.pro.txboard.exception.MethodNotImplementedException;
import com.sdlc.pro.txboard.model.*;
import com.sdlc.pro.txboard.redis.RedisJsonOperation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.InitializingBean;

import java.time.Duration;
import java.time.Instant;
import java.util.LinkedList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;


public final class RedisTransactionLogRepository implements TransactionLogRepository, InitializingBean {
    private static final Logger log = LoggerFactory.getLogger(RedisTransactionLogRepository.class);

    private final RedisJsonOperation redisJsonOperation;
    private final TxBoardProperties txBoardProperties;

    public RedisTransactionLogRepository(RedisJsonOperation redisJsonOperation, TxBoardProperties txBoardProperties) {
        this.redisJsonOperation = redisJsonOperation;
        this.txBoardProperties = txBoardProperties;
    }

    @Override
    public void save(TransactionLog transactionLog) {
        RedisTransactionLog redisTransactionLog = toRedisTransactionLog(transactionLog);
        Duration ttl = this.txBoardProperties.getRedis().getEntityTtl();
        String key = this.redisJsonOperation.saveWithExpire(redisTransactionLog, ttl.toSeconds());
        log.debug("Redis entity saved. key: {}, TTL: {}",key, ttl);
    }

    @Override
    public List<TransactionLog> findAll() {
        throw new MethodNotImplementedException();
    }

    @Override
    public long count() {
        return this.redisJsonOperation.count(RedisTransactionLog.class);
    }

    @Override
    public long countByTransactionStatus(TransactionPhaseStatus status) {
        return this.redisJsonOperation.countByFieldValue(RedisTransactionLog.class, "status", status);
    }

    @Override
    public double averageDuration() {
        throw new MethodNotImplementedException();
    }

    @Override
    public PageResponse<TransactionLog> findAll(PageRequest request) {
        PageResponse<RedisTransactionLog> pageResponse = this.redisJsonOperation.findPageable(RedisTransactionLog.class, request);
        List<TransactionLog> content = pageResponse.getContent()
                .stream()
                .map(this::toTransactionLog)
                .toList();

        return new PageResponse<>(content, request, pageResponse.getTotalElements());
    }

    @Override
    public TransactionSummary getTransactionSummary() {
        Class<?> entityType = RedisTransactionLog.class;
        long commitCount = this.countByTransactionStatus(TransactionPhaseStatus.COMMITTED);
        long rolledBackCount = this.countByTransactionStatus(TransactionPhaseStatus.ROLLED_BACK);
        long erroredCount = this.countByTransactionStatus(TransactionPhaseStatus.ERRORED);
        long totalDuration = (long) this.redisJsonOperation.sum(entityType, "duration");
        long alarmingCount = 0L; // this.redisJsonOperation.countByFieldValue(entityType, "alarmingTransaction", true);
        long connectionAcquisitionCount = (long) this.redisJsonOperation.sum(entityType, "connectionSummary.acquisitionCount");
        long totalConnectionOccupiedTime = (long) this.redisJsonOperation.sum(entityType, "connectionSummary.occupiedTime");
        long alarmingConnectionCount = 0L; // (long) this.redisJsonOperation.sum(entityType, "connectionSummary.alarmingConnectionCount");

        return new TransactionSummary(
                commitCount,
                rolledBackCount,
                erroredCount,
                totalDuration,
                alarmingCount,
                connectionAcquisitionCount,
                totalConnectionOccupiedTime,
                alarmingConnectionCount
        );
    }

    @Override
    public List<DurationDistribution> getDurationDistributions() {
        Class<?> entityType = RedisTransactionLog.class;
        List<Integer> buckets = txBoardProperties.getDurationBuckets();
        List<DurationDistribution> distributions = new LinkedList<>();

        int prev = 0;
        for (int curr : buckets) {
            long count = this.redisJsonOperation.countByRange(entityType, "duration", prev, curr);
            distributions.add(new DurationDistribution(new DurationRange(prev, curr), count));
            prev = curr + 1;
        }

        if (prev > 0) {
            long count = this.redisJsonOperation.countByRange(entityType, "duration", prev, Integer.MAX_VALUE);
            if (count != 0) {
                distributions.add(new DurationDistribution(new DurationRange(prev, Integer.MAX_VALUE), count));
            }
        }

        return distributions;
    }

    @Override
    public void afterPropertiesSet() throws Exception {
        this.prepareSchema();
        log.info("The RedisTransactionLogRepository has been created and initialized to support Redis as a storage of transaction logs.");
    }

    private void prepareSchema() {
        // register and create index for RedisTransactionLog
        this.redisJsonOperation.registerRedisEntityClass(RedisTransactionLog.class);
        this.redisJsonOperation.createIndex(RedisTransactionLog.class);
    }

    private RedisTransactionLog toRedisTransactionLog(TransactionLog transactionLog) {
        RedisTransactionLog redisTransactionLog = new RedisTransactionLog();
        redisTransactionLog.setTxId(Optional.ofNullable(transactionLog.getTxId()).map(UUID::toString).orElse(null));
        redisTransactionLog.setMethod(transactionLog.getMethod());
        redisTransactionLog.setPropagation(transactionLog.getPropagation().name());
        redisTransactionLog.setIsolation(transactionLog.getIsolation().name());
        redisTransactionLog.setStartTime(transactionLog.getStartTime().toEpochMilli());
        redisTransactionLog.setEndTime(transactionLog.getEndTime().toEpochMilli());
        redisTransactionLog.setDuration(transactionLog.getDuration());
        redisTransactionLog.setConnectionSummary(transactionLog.getConnectionSummary());
        redisTransactionLog.setConnectionOriented(transactionLog.getConnectionOriented());
        redisTransactionLog.setStatus(transactionLog.getStatus().name());
        redisTransactionLog.setThread(transactionLog.getThread());
        redisTransactionLog.setExecutedQuires(transactionLog.getExecutedQuires());
        redisTransactionLog.setEvents(transactionLog.getEvents());
        redisTransactionLog.setAlarmingTransaction(transactionLog.isAlarmingTransaction());
        redisTransactionLog.setHavingAlarmingConnection(transactionLog.getHavingAlarmingConnection());
        redisTransactionLog.setPostTransactionQuires(transactionLog.getPostTransactionQuires());

        List<RedisTransactionLog> child = transactionLog.getChild()
                .stream()
                .map(this::toRedisTransactionLog)
                .toList();

        redisTransactionLog.setChild(child);

        return redisTransactionLog;
    }

    private TransactionLog toTransactionLog(RedisTransactionLog redisTransactionLog) {
        List<TransactionLog> child = redisTransactionLog.getChild()
                .stream()
                .map(this::toTransactionLog)
                .toList();

        return new TransactionLog(
                Optional.ofNullable(redisTransactionLog.getTxId()).map(UUID::fromString).orElse(null),
                redisTransactionLog.getMethod(),
                PropagationBehavior.valueOf(redisTransactionLog.getPropagation()),
                IsolationLevel.valueOf(redisTransactionLog.getIsolation()),
                Instant.ofEpochMilli(redisTransactionLog.getStartTime()),
                Instant.ofEpochMilli(redisTransactionLog.getEndTime()),
                redisTransactionLog.getConnectionSummary(),
                TransactionPhaseStatus.valueOf(redisTransactionLog.getStatus()),
                redisTransactionLog.getThread(),
                redisTransactionLog.getExecutedQuires(),
                child,
                redisTransactionLog.getEvents(),
                this.txBoardProperties.getAlarmingThreshold().getTransaction(),
                redisTransactionLog.getPostTransactionQuires()
        );
    }
}
