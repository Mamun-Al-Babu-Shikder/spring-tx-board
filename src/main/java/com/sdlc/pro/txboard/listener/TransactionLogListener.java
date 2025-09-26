package com.sdlc.pro.txboard.listener;

import com.sdlc.pro.txboard.model.TransactionLog;

public interface TransactionLogListener {
    void listen(TransactionLog transactionLog);
}
