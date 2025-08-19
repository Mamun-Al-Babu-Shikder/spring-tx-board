package com.sdlc.pro.txboard.proxy;

import com.sdlc.pro.txboard.listener.TransactionPhaseListener;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.TransactionException;
import org.springframework.transaction.TransactionStatus;

public class PlatformTransactionManagerProxy implements PlatformTransactionManager {
    private final PlatformTransactionManager transactionManager;
    private final TransactionPhaseListener transactionPhaseListener;

    public PlatformTransactionManagerProxy(PlatformTransactionManager transactionManager, TransactionPhaseListener transactionPhaseListener) {
        this.transactionManager = transactionManager;
        this.transactionPhaseListener = transactionPhaseListener;
    }

    @Override
    public TransactionStatus getTransaction(TransactionDefinition definition) throws TransactionException {
        this.transactionPhaseListener.beforeBegin(definition);
        try {
            TransactionStatus transactionStatus = transactionManager.getTransaction(definition);
            this.transactionPhaseListener.afterBegin(transactionStatus, null);
            return transactionStatus;
        } catch (Throwable throwable) {
            this.transactionPhaseListener.afterBegin(null, throwable);
            throw throwable;
        }
    }

    @Override
    public void commit(TransactionStatus status) throws TransactionException {
        try {
            this.transactionManager.commit(status);
            this.transactionPhaseListener.afterCommit();
        } catch (Throwable throwable) {
            this.transactionPhaseListener.errorOccurredAtTransactionPhase(throwable);
        }
    }

    @Override
    public void rollback(TransactionStatus status) throws TransactionException {
        try {
            this.transactionManager.rollback(status);
            this.transactionPhaseListener.afterRollback();
        } catch (Throwable throwable) {
            this.transactionPhaseListener.errorOccurredAtTransactionPhase(throwable);
        }
    }
}
