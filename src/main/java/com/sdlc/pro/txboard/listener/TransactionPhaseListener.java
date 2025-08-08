package com.sdlc.pro.txboard.listener;

import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.TransactionStatus;

public sealed interface TransactionPhaseListener permits TransactionPhaseListenerImpl {

    default void beforeBegin(TransactionDefinition definition) {
    }

    default void afterBegin(TransactionStatus transactionStatus, Throwable throwable) {
    }

    default void afterCommit() {
    }

    default void afterRollback() {
    }

    default void afterAcquiredConnection() {
    }

    default void afterCloseConnection() {

    }

    default void errorOccurredAtTransactionPhase(Throwable throwable) {
    }
}
