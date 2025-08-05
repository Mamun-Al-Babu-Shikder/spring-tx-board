package com.sdlc.pro.txboard.listener;

import org.springframework.transaction.TransactionDefinition;

public interface TransactionPhaseListener {

    default void beforeBegin(TransactionDefinition definition) {
    }

    default void afterBegin(boolean isNewTransaction) {
    }

    default void afterCommit() {
    }

    default void afterRollback() {
    }

    default void afterAcquiredConnection() {
    }

    default void afterCloseConnection() {

    }
}
