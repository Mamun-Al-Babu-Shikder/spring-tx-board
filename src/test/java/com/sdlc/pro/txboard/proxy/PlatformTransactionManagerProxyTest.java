package com.sdlc.pro.txboard.proxy;

import com.sdlc.pro.txboard.listener.TransactionPhaseListener;
import com.sdlc.pro.txboard.listener.TransactionPhaseListenerImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InOrder;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.TransactionSystemException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;

class PlatformTransactionManagerProxyTest {

    private PlatformTransactionManager transactionManager;
    private TransactionPhaseListener transactionPhaseListener;
    private PlatformTransactionManagerProxy transactionManagerProxy;

    @BeforeEach
    void setup() {
        transactionManager = mock(PlatformTransactionManager.class);
        transactionPhaseListener = mock(TransactionPhaseListener.class);
        transactionManagerProxy = new PlatformTransactionManagerProxy(transactionManager, transactionPhaseListener);
    }

    @Test
    void shouldNotifyToListenerBeforeAndAfterBeginOnSuccess() {
        TransactionDefinition definition = mock(TransactionDefinition.class);
        TransactionStatus status = mock(TransactionStatus.class);

        when(transactionManager.getTransaction(definition)).thenReturn(status);

        TransactionStatus currentStatus = transactionManagerProxy.getTransaction(definition);

        InOrder inOrder = inOrder(transactionPhaseListener, transactionManager);
        inOrder.verify(transactionPhaseListener, times(1)).beforeBegin(definition);
        inOrder.verify(transactionManager, times(1)).getTransaction(definition);
        inOrder.verify(transactionPhaseListener, times(1)).afterBegin(null);

        assertEquals(status, currentStatus);
    }

    @Test
    void shouldNotifyToListenerBeforeAndAfterBeginOnFailure() {
        TransactionDefinition definition = mock(TransactionDefinition.class);

        RuntimeException exception = new RuntimeException("tx failed!");
        when(transactionManager.getTransaction(definition)).thenThrow(exception);

        assertThrows(RuntimeException.class, () -> transactionManagerProxy.getTransaction(definition));

        InOrder orderInvocations = inOrder(transactionPhaseListener, transactionManager);
        orderInvocations.verify(transactionPhaseListener, times(1)).beforeBegin(definition);
        orderInvocations.verify(transactionManager, times(1)).getTransaction(definition);
        orderInvocations.verify(transactionPhaseListener, times(1)).afterBegin(exception);
    }

    @Test
    void shouldNotifyToListenerAfterCommitOnSuccess() {
        TransactionStatus status = mock(TransactionStatus.class);
        transactionManagerProxy.commit(status);

        InOrder orderInvocations = inOrder(transactionManager, transactionPhaseListener);
        orderInvocations.verify(transactionManager, times(1)).commit(status);
        orderInvocations.verify(transactionPhaseListener, times(1)).afterCommit();
    }

    @Test
    void shouldNotifyToListenerErrorOccurredWhenCommitOnFailure() {
        TransactionStatus status = mock(TransactionStatus.class);

        TransactionSystemException exception = new TransactionSystemException("Failed to commit the transaction!");
        doThrow(exception).when(transactionManager).commit(status);

        try {
            transactionManagerProxy.commit(status);
        } catch (Exception ignore) {
        }

        verify(transactionPhaseListener).errorOccurredAtTransactionPhase(exception);
    }

    @Test
    void shouldNotifyToListenerAfterRollbackOnSuccess() {
        TransactionStatus status = mock(TransactionStatus.class);
        transactionManagerProxy.rollback(status);

        InOrder orderInvocations = inOrder(transactionManager, transactionPhaseListener);
        orderInvocations.verify(transactionManager, times(1)).rollback(status);
        orderInvocations.verify(transactionPhaseListener, times(1)).afterRollback();
    }

    @Test
    void shouldNotifyToListenerAsRollbackTransactionWhenSetTransactionRollbackOnly() {
        TransactionStatus status = mock(TransactionStatus.class);
        when(status.isRollbackOnly()).thenReturn(true);
        transactionManagerProxy.commit(status);

        InOrder orderInvocations = inOrder(transactionManager, transactionPhaseListener);
        orderInvocations.verify(transactionManager, times(1)).commit(status);
        orderInvocations.verify(transactionPhaseListener, times(1)).afterRollback();
    }

    @Test
    void shouldNotifyToListenerErrorOccurredWhenRollbackOnFailure() {
        TransactionStatus status = mock(TransactionStatus.class);

        RuntimeException exception = new RuntimeException("rollback failed!");
        doThrow(exception).when(transactionManager).rollback(status);

        try {
            transactionManagerProxy.rollback(status);
        } catch (Exception ignore) {
        }

        verify(transactionPhaseListener).errorOccurredAtTransactionPhase(exception);
    }
}
