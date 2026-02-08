package com.sdlc.pro.txboard.repository;

import com.sdlc.pro.txboard.config.TxBoardProperties;
import com.sdlc.pro.txboard.domain.PageRequest;
import com.sdlc.pro.txboard.domain.PageResponse;
import com.sdlc.pro.txboard.model.RedisSqlExecutionLog;
import com.sdlc.pro.txboard.model.SqlExecutionLog;
import com.sdlc.pro.txboard.redis.RedisJsonOperation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.InitializingBean;

import java.time.Instant;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

public class RedisSqlExecutionLogRepository implements SqlExecutionLogRepository, InitializingBean {
    private static final Logger log = LoggerFactory.getLogger(RedisSqlExecutionLogRepository.class);

    private final RedisJsonOperation redisJsonOperation;
    private final TxBoardProperties txBoardProperties;

    public RedisSqlExecutionLogRepository(RedisJsonOperation redisJsonOperation, TxBoardProperties txBoardProperties) {
        this.redisJsonOperation = redisJsonOperation;
        this.txBoardProperties = txBoardProperties;
    }

    @Override
    public void save(SqlExecutionLog sqlExecutionLog) {
        this.redisJsonOperation.saveWithExpire(
                this.toRedisSqlExecutionLog(sqlExecutionLog),
                this.txBoardProperties.getRedis().getEntityTtl().getSeconds()
        );
    }

    @Override
    public PageResponse<SqlExecutionLog> findAll(PageRequest request) {
        PageResponse<RedisSqlExecutionLog> pageResponse = this.redisJsonOperation.findPageable(RedisSqlExecutionLog.class, request);
        List<SqlExecutionLog> content = pageResponse.getContent()
                .stream()
                .map(this::toSqlExecutionLog)
                .collect(Collectors.toList());

        return new PageResponse<>(content, request, pageResponse.getTotalElements());
    }

    @Override
    public void afterPropertiesSet() throws Exception {
        this.prepareSchema();
        log.info("The RedisSqlExecutionLogRepository has been created and initialized to support redis storage of sql execution logs.");
    }

    private void prepareSchema() {
        // register and create index for RedisSqlExecutionLog
        this.redisJsonOperation.registerRedisEntityClass(RedisSqlExecutionLog.class);
        this.redisJsonOperation.createIndex(RedisSqlExecutionLog.class);
    }

    private RedisSqlExecutionLog toRedisSqlExecutionLog(SqlExecutionLog sqlExecutionLog) {
        RedisSqlExecutionLog redisSqlExecutionLog = new RedisSqlExecutionLog();
        redisSqlExecutionLog.setId(sqlExecutionLog.getId().toString());
        redisSqlExecutionLog.setConAcquiredTime(sqlExecutionLog.getConAcquiredTime().toEpochMilli());
        redisSqlExecutionLog.setConReleaseTime(sqlExecutionLog.getConReleaseTime().toEpochMilli());
        redisSqlExecutionLog.setConOccupiedTime(sqlExecutionLog.getConOccupiedTime());
        redisSqlExecutionLog.setAlarmingConnection(sqlExecutionLog.isAlarmingConnection());
        redisSqlExecutionLog.setThread(sqlExecutionLog.getThread());
        redisSqlExecutionLog.setExecutedQuires(sqlExecutionLog.getExecutedQuires());
        return redisSqlExecutionLog;
    }

    private SqlExecutionLog toSqlExecutionLog(RedisSqlExecutionLog redisSqlExecutionLog) {
        return new SqlExecutionLog(
                UUID.fromString(redisSqlExecutionLog.getId()),
                Instant.ofEpochMilli(redisSqlExecutionLog.getConAcquiredTime()),
                Instant.ofEpochMilli(redisSqlExecutionLog.getConReleaseTime()),
                redisSqlExecutionLog.isAlarmingConnection(),
                redisSqlExecutionLog.getThread(),
                redisSqlExecutionLog.getExecutedQuires()
        );
    }
}
