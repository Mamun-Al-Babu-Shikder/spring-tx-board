package com.sdlc.pro.txboard.listener;

import org.springframework.transaction.TransactionDefinition;

public interface TransactionPhaseListener {

    default void beforeBegin(TransactionDefinition definition) {
    }

    default void afterBegin(Throwable throwable) {
    }

    default void afterCommit() {
    }

    default void afterRollback() {
    }

    default void afterAcquiredConnection() {
    }

    default void afterCloseConnection() {
    }

    default void executedQuery(String query) {
    }

    default void errorOccurredAtTransactionPhase(Throwable throwable) {
    }
}
