package com.sdlc.pro.txboard.repository;

import com.sdlc.pro.txboard.enums.TransactionPhaseStatus;
import com.sdlc.pro.txboard.domain.PageRequest;
import com.sdlc.pro.txboard.domain.PageResponse;
import com.sdlc.pro.txboard.model.DurationDistribution;
import com.sdlc.pro.txboard.model.TransactionLog;
import com.sdlc.pro.txboard.model.TransactionSummary;

import java.util.List;

public interface TransactionLogRepository {
    void save(TransactionLog transactionLog);
    List<TransactionLog> findAll();
    long count();
    long countByTransactionStatus(TransactionPhaseStatus status);
    double averageDuration();
    PageResponse findAll(PageRequest request);
    TransactionSummary getTransactionSummary();
    List<DurationDistribution> getDurationDistributions();
}
