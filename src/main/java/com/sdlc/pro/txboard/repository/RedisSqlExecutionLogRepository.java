package com.sdlc.pro.txboard.repository;

import com.sdlc.pro.txboard.domain.PageRequest;
import com.sdlc.pro.txboard.domain.PageResponse;
import com.sdlc.pro.txboard.exception.MethodNotImplementedException;
import com.sdlc.pro.txboard.model.SqlExecutionLog;
import com.sdlc.pro.txboard.redis.RedisJsonOperation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.InitializingBean;

public class RedisSqlExecutionLogRepository implements SqlExecutionLogRepository, InitializingBean {
    private static final Logger log = LoggerFactory.getLogger(RedisSqlExecutionLogRepository.class);

    private final RedisJsonOperation redisJsonOperation;

    public RedisSqlExecutionLogRepository(RedisJsonOperation redisJsonOperation) {
        this.redisJsonOperation = redisJsonOperation;
    }

    @Override
    public void save(SqlExecutionLog sqlExecutionLog) {
        throw new MethodNotImplementedException();
    }

    @Override
    public PageResponse<SqlExecutionLog> findAll(PageRequest request) {
        throw new MethodNotImplementedException();
    }

    @Override
    public void afterPropertiesSet() throws Exception {
        log.info("The RedisSqlExecutionLogRepository has been created and initialized to support redis storage of sql execution logs.");
    }
}
