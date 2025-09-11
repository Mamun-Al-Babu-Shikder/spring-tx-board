package com.sdlc.pro.txboard.proxy;

import com.sdlc.pro.txboard.listener.TransactionPhaseListener;
import com.sdlc.pro.txboard.listener.TransactionPhaseListenerImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.SQLException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class DataSourceProxyTest {
    private DataSource dataSource;
    private TransactionPhaseListener transactionPhaseListener;
    private DataSourceProxy dataSourceProxy;

    @BeforeEach
    void setup() throws SQLException {
        dataSource = mock(DataSource.class);
        Connection connection = mock(Connection.class);
        transactionPhaseListener = mock(TransactionPhaseListenerImpl.class);

        when(dataSource.getConnection()).thenReturn(connection);
        when(dataSource.getConnection(any(), any())).thenReturn(connection);

        dataSourceProxy = new DataSourceProxy(dataSource, transactionPhaseListener);
    }

    @Test
    void shouldWrapConnectionAndNotifyListenerOnDefaultGetConnection() throws SQLException {
        Connection proxyConnection = dataSourceProxy.getConnection();
        assertThat(proxyConnection).isInstanceOf(ConnectionProxy.class);
        verify(transactionPhaseListener, times(1)).afterAcquiredConnection();
    }

    @Test
    void shouldWrapConnectionAndNotifyListenerOnGetConnectionWithCredentials() throws SQLException {
        Connection proxyConnection = dataSourceProxy.getConnection("username", "password");
        assertThat(proxyConnection).isInstanceOf(ConnectionProxy.class);
        verify(transactionPhaseListener, times(1)).afterAcquiredConnection();
    }

    @Test
    void shouldDelegateToActualConnectionCreation() throws SQLException {
        dataSourceProxy.getConnection();
        verify(dataSource, times(1)).getConnection();

        dataSourceProxy.getConnection("username", "password");
        verify(dataSource, times(1)).getConnection("username", "password");
    }
}
