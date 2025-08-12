package com.sdlc.pro.txboard.listener;

import com.sdlc.pro.txboard.model.TransactionLog;

public sealed interface TransactionLogListener permits TransactionLogPersistenceListener {
    void listen(TransactionLog transactionLog);
}
