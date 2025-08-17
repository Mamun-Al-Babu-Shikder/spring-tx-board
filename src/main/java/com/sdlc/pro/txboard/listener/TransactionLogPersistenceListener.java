package com.sdlc.pro.txboard.listener;

import com.sdlc.pro.txboard.model.TransactionLog;
import com.sdlc.pro.txboard.repository.TransactionLogRepository;

public final class TransactionLogPersistenceListener implements TransactionLogListener {
    private final TransactionLogRepository repository;

    public TransactionLogPersistenceListener(TransactionLogRepository repository) {
        this.repository = repository;
    }

    @Override
    public void listen(TransactionLog transactionLog) {
        repository.save(transactionLog);
    }
}
