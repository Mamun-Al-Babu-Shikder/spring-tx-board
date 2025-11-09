package com.sdlc.pro.txboard.repository;

import com.sdlc.pro.txboard.enums.TransactionPhaseStatus;
import com.sdlc.pro.txboard.domain.TransactionLogPageRequest;
import com.sdlc.pro.txboard.domain.TransactionLogPageResponse;
import com.sdlc.pro.txboard.model.DurationDistribution;
import com.sdlc.pro.txboard.model.TransactionLog;
import com.sdlc.pro.txboard.model.TransactionSummary;

import java.util.List;

public sealed interface TransactionLogRepository permits InMemoryTransactionLogRepository, RedisTransactionLogRepository {
    void save(TransactionLog transactionLog);
    List<TransactionLog> findAll();
    long count();
    long countByTransactionStatus(TransactionPhaseStatus status);
    double averageDuration();
    TransactionLogPageResponse findAll(TransactionLogPageRequest request);
    TransactionSummary getTransactionSummary();
    List<DurationDistribution> getDurationDistributions();
    void deleteAll();
}
