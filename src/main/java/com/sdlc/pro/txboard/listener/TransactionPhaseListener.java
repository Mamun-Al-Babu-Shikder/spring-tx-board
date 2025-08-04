package com.sdlc.pro.txboard.listener;


import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.TransactionStatus;

public interface TransactionPhaseListener {

    default void beforeBegin(TransactionDefinition definition, TransactionStatus status) {
    }

    default void afterCommit(TransactionStatus status) {
    }

    default void afterRollback(TransactionStatus status) {
    }

    default void afterAcquiredConnection() {
    }

    default void afterCloseConnection() {

    }
}
