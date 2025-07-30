package com.sdlc.pro.txboard.repository;

import com.sdlc.pro.txboard.enums.TransactionStatus;
import com.sdlc.pro.txboard.domain.TransactionLogPageRequest;
import com.sdlc.pro.txboard.domain.TransactionLogPageResponse;
import com.sdlc.pro.txboard.model.DurationDistribution;
import com.sdlc.pro.txboard.model.TransactionLog;

import java.util.List;

public sealed interface TransactionLogRepository permits InMemoryTransactionLogRepository, RedisTransactionLogRepository {
    void save(TransactionLog transactionLog);
    List<TransactionLog> findAll();
    long count();
    long countByTransactionStatus(TransactionStatus status);
    double averageDuration();
    TransactionLogPageResponse findAll(TransactionLogPageRequest request);
    List<DurationDistribution> getDurationDistributions();
}
