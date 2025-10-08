package com.sdlc.pro.txboard.proxy;

import com.sdlc.pro.txboard.listener.TransactionPhaseListener;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.junit.jupiter.api.extension.ExtendWith;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.SQLException;

import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class DataSourceProxyTest {

    @Mock
    private DataSource dataSource;

    @Mock
    private Connection connection;

    @Mock
    private TransactionPhaseListener transactionPhaseListener;

    private DataSourceProxy dataSourceProxy;

    @BeforeEach
    void setUp() throws SQLException {
        lenient().when(dataSource.getConnection()).thenReturn(connection);
        lenient().when(dataSource.getConnection("user", "pass")).thenReturn(connection);
        dataSourceProxy = new DataSourceProxy(dataSource, transactionPhaseListener);
    }

    @Test
    void testGetConnection_DelegatesAndTriggersListener() throws Exception {
        Connection result = dataSourceProxy.getConnection();

        verify(dataSource).getConnection();
        verify(transactionPhaseListener).afterAcquiredConnection();

        result.close();
        verify(transactionPhaseListener).afterCloseConnection();
    }

    @Test
    void testGetConnectionWithCredentials_DelegatesAndTriggersListener() throws Exception {
        Connection result = dataSourceProxy.getConnection("user", "pass");

        verify(dataSource).getConnection("user", "pass");
        verify(transactionPhaseListener).afterAcquiredConnection();

        result.close();
        verify(transactionPhaseListener).afterCloseConnection();
    }
}
