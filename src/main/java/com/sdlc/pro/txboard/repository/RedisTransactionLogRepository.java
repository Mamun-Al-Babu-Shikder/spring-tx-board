package com.sdlc.pro.txboard.repository;

import com.sdlc.pro.txboard.domain.PageRequest;
import com.sdlc.pro.txboard.domain.TransactionLogPageResponse;
import com.sdlc.pro.txboard.enums.TransactionPhaseStatus;
import com.sdlc.pro.txboard.exception.MethodNotImplementedException;
import com.sdlc.pro.txboard.model.DurationDistribution;
import com.sdlc.pro.txboard.model.TransactionLog;
import com.sdlc.pro.txboard.model.TransactionSummary;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.InitializingBean;

import java.util.List;

public final class RedisTransactionLogRepository implements TransactionLogRepository, InitializingBean {
    private static final Logger log = LoggerFactory.getLogger(RedisTransactionLogRepository.class);

    @Override
    public void save(TransactionLog transactionLog) {
        throw new MethodNotImplementedException();
    }

    @Override
    public List<TransactionLog> findAll() {
        throw new MethodNotImplementedException();
    }

    @Override
    public long count() {
        throw new MethodNotImplementedException();
    }

    @Override
    public long countByTransactionStatus(TransactionPhaseStatus status) {
        throw new MethodNotImplementedException();
    }

    @Override
    public double averageDuration() {
        throw new MethodNotImplementedException();
    }

    @Override
    public TransactionLogPageResponse findAll(PageRequest request) {
        throw new MethodNotImplementedException();
    }

    @Override
    public TransactionSummary getTransactionSummary() {
        throw new MethodNotImplementedException();
    }

    @Override
    public List<DurationDistribution> getDurationDistributions() {
        throw new MethodNotImplementedException();
    }

    @Override
    public void afterPropertiesSet() throws Exception {
        log.info("The RedisTransactionLogRepository has been created and initialized to support Redis as a storage of transaction logs.");
    }
}
