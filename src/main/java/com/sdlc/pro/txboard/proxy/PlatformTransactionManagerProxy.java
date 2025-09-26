package com.sdlc.pro.txboard.proxy;

import com.sdlc.pro.txboard.listener.TransactionPhaseListener;
import org.springframework.dao.UncategorizedDataAccessException;
import org.springframework.transaction.*;

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
            this.transactionPhaseListener.afterBegin(null);
            return transactionStatus;
        } catch (Throwable throwable) {
            this.transactionPhaseListener.afterBegin(throwable);
            throw throwable;
        }
    }

    @Override
    public void commit(TransactionStatus status) throws TransactionException {
        try {
            this.transactionManager.commit(status);
            if (status.isRollbackOnly()) {
                this.transactionPhaseListener.afterRollback();
            } else {
                this.transactionPhaseListener.afterCommit();
            }
        } catch (UnexpectedRollbackException exception) {
            this.transactionPhaseListener.afterRollback();
            throw exception;
        } catch (TransactionException | UncategorizedDataAccessException exception) {
            this.transactionPhaseListener.errorOccurredAtTransactionPhase(exception);
            throw exception;
        } catch (RuntimeException | Error e) {
            this.transactionPhaseListener.afterRollback();
            throw e;
        } catch (Throwable throwable) {
            this.transactionPhaseListener.errorOccurredAtTransactionPhase(throwable);
            throw throwable;
        }
    }

    @Override
    public void rollback(TransactionStatus status) throws TransactionException {
        try {
            this.transactionManager.rollback(status);
            this.transactionPhaseListener.afterRollback();
        } catch (Throwable throwable) {
            this.transactionPhaseListener.errorOccurredAtTransactionPhase(throwable);
            throw throwable;
        }
    }
}
