package com.sdlc.pro.txboard.proxy;

import com.sdlc.pro.txboard.listener.TransactionPhaseListener;
import com.sdlc.pro.txboard.listener.TransactionPhaseListenerImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

class ConnectionProxyTest {
    private Connection connection;
    private TransactionPhaseListener transactionPhaseListener;
    private ConnectionProxy connectionProxy;

    @BeforeEach
    void setup() throws SQLException {
        connection = mock(Connection.class);
        Statement statement = mock(Statement.class);
        transactionPhaseListener = mock(TransactionPhaseListenerImpl.class);

        when(connection.createStatement()).thenReturn(statement);

        connectionProxy = new ConnectionProxy(connection, transactionPhaseListener);
    }

    @Test
    void shouldCreateStatementProxyWithTransactionPhaseListener() throws SQLException {
        Statement proxyStatement = connectionProxy.createStatement();
        assertThat(proxyStatement).isInstanceOf(StatementProxy.class);
        assertThat(proxyStatement).extracting("transactionPhaseListener").isEqualTo(transactionPhaseListener);
    }

    @Test
    void shouldDelegateToActualStatementCreation() throws SQLException {
        connectionProxy.createStatement();
        verify(connection).createStatement();
    }

    @Test
    void shouldNotifyTheSqlToListenerAndInvokeActualPrepareStatementMethod() throws SQLException {
        String SQL = "select * from product";
        connectionProxy.prepareStatement(SQL);
        verify(transactionPhaseListener, times(1)).executedQuery(SQL);
        verify(connection, times(1)).prepareStatement(SQL);
    }

    @Test
    void shouldNotifyTheSqlToListenerAndInvokeActualPrepareCallMethod() throws SQLException {
        String SQL = "{CALL getProductDetails(?, ?, ?)}";
        connectionProxy.prepareCall(SQL);
        verify(transactionPhaseListener, times(1)).executedQuery(SQL);
        verify(connection, times(1)).prepareCall(SQL);
    }

    @Test
    void shouldNotifyTheSqlToListenerAndInvokeActualNativeSQLMethod() throws SQLException {
        String SQL = "select * from product";
        connectionProxy.nativeSQL(SQL);
        verify(transactionPhaseListener, times(1)).executedQuery(SQL);
        verify(connection, times(1)).nativeSQL(SQL);
    }

    @Test
    void shouldNotifyTheSqlToListenerAndInvokeActualPrepareStatement3ParamMethod() throws SQLException {
        String SQL = "select * from product where category = ?";
        int resultSetType = ResultSet.TYPE_SCROLL_SENSITIVE;
        int resultSetConcurrency = ResultSet.CONCUR_READ_ONLY;
        connectionProxy.prepareStatement(SQL, resultSetType, resultSetConcurrency);
        verify(transactionPhaseListener, times(1)).executedQuery(SQL);
        verify(connection, times(1)).prepareStatement(SQL, resultSetType, resultSetConcurrency);
    }

    @Test
    void shouldNotifyTheSqlToListenerAndInvokeActualPrepareStatement4ParamMethod() throws SQLException {
        String SQL = "select * from product where category = ?";
        int resultSetType = ResultSet.TYPE_SCROLL_SENSITIVE;
        int resultSetConcurrency = ResultSet.CONCUR_UPDATABLE;
        int resultSetHoldability = ResultSet.HOLD_CURSORS_OVER_COMMIT;
        connectionProxy.prepareStatement(SQL, resultSetType, resultSetConcurrency, resultSetHoldability);
        verify(transactionPhaseListener, times(1)).executedQuery(SQL);
        verify(connection, times(1)).prepareStatement(SQL, resultSetType, resultSetConcurrency, resultSetHoldability);
    }

    @Test
    void shouldNotifyTheSqlToListenerAndInvokeActualPrepareCall3ParamMethod() throws SQLException {
        String SQL = "{CALL getProductDetails(?, ?, ?)}";
        int resultSetType = ResultSet.TYPE_SCROLL_SENSITIVE;
        int resultSetConcurrency = ResultSet.CONCUR_READ_ONLY;
        connectionProxy.prepareCall(SQL, resultSetType, resultSetConcurrency);
        verify(transactionPhaseListener, times(1)).executedQuery(SQL);
        verify(connection, times(1)).prepareCall(SQL, resultSetType, resultSetConcurrency);
    }

    @Test
    void shouldNotifyTheSqlToListenerAndInvokeActualPrepareCall4ParamMethod() throws SQLException {
        String SQL = "{CALL getProductDetails(?, ?, ?)}";
        int resultSetType = ResultSet.TYPE_SCROLL_SENSITIVE;
        int resultSetConcurrency = ResultSet.CONCUR_READ_ONLY;
        int resultSetHoldability = ResultSet.HOLD_CURSORS_OVER_COMMIT;
        connectionProxy.prepareCall(SQL, resultSetType, resultSetConcurrency, resultSetHoldability);
        verify(transactionPhaseListener, times(1)).executedQuery(SQL);
        verify(connection, times(1)).prepareCall(SQL, resultSetType, resultSetConcurrency, resultSetHoldability);
    }

    @Test
    void shouldNotifyTheSqlToListenerAndInvokeActualPrepareStatementKeyGenParamMethod() throws SQLException {
        String SQL = "insert into users (name, email) values (?, ?)";
        int autoGeneratedKeys = Statement.RETURN_GENERATED_KEYS;
        connectionProxy.prepareStatement(SQL, autoGeneratedKeys);
        verify(transactionPhaseListener, times(1)).executedQuery(SQL);
        verify(connection, times(1)).prepareStatement(SQL, autoGeneratedKeys);
    }

    @Test
    void shouldNotifyTheSqlToListenerAndInvokeActualPrepareStatementColIndexesParamMethod() throws SQLException {
        String SQL = "insert into users (name, email) values (?, ?)";
        int[] keyColumnIndexes = {1};
        connectionProxy.prepareStatement(SQL, keyColumnIndexes);
        verify(transactionPhaseListener, times(1)).executedQuery(SQL);
        verify(connection, times(1)).prepareStatement(SQL, keyColumnIndexes);
    }

    @Test
    void shouldNotifyTheSqlToListenerAndInvokeActualPrepareStatementColNamesParamMethod() throws SQLException {
        String SQL = "insert into users (name, email) values (?, ?)";
        String[] generatedColumns = {"id", "creation_timestamp"};
        connectionProxy.prepareStatement(SQL, generatedColumns);
        verify(transactionPhaseListener, times(1)).executedQuery(SQL);
        verify(connection, times(1)).prepareStatement(SQL, generatedColumns);
    }

    @Test
    void shouldCloseActualConnectionAndNotifyTheTransactionPhaseListener() throws SQLException {
        connectionProxy.close();
        verify(connection, times(1)).close();
        verify(transactionPhaseListener, times(1)).afterCloseConnection();
    }
}
