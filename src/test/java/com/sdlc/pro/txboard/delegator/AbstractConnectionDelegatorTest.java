package com.sdlc.pro.txboard.delegator;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.sql.*;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.Executor;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AbstractConnectionDelegatorTest {

    static class Delegator extends AbstractConnectionDelegator {
        Delegator(Connection conn) {
            super(conn);
        }
    }

    interface Parent {}

    static class Child implements Parent {}

    String sql = "SELECT 1 FROM DUAL";
    String testString = "-test-";
    int testInt = 1000;

    @Mock
    Connection connection;

    AbstractConnectionDelegator delegator;

    @BeforeEach
    void setUp() {
        delegator = new Delegator(connection);
    }

    @Test
    void createStatement_shouldCreateStatement() throws SQLException {
        when(delegator.createStatement()).thenReturn(mock(Statement.class));
        assertNotNull(delegator.createStatement());
        verify(connection).createStatement();
    }

    @Test
    void prepareStatement_shouldCreatePreparedStatement() throws SQLException {
        when(delegator.prepareStatement(sql)).thenReturn(mock(PreparedStatement.class));
        assertNotNull(delegator.prepareStatement(sql));
        verify(connection).prepareStatement(sql);
    }

    @Test
    void prepareCall_shouldCreateCallableStatement() throws SQLException {
        when(delegator.prepareCall(sql)).thenReturn(mock(CallableStatement.class));
        assertNotNull(delegator.prepareCall(sql));
        verify(connection).prepareCall(sql);
    }

    @Test
    void nativeSQL_shouldReturnNativeSQL() throws SQLException {
        when(delegator.nativeSQL(sql)).thenReturn(testString);
        assertEquals(testString, delegator.nativeSQL(sql));
        verify(connection).nativeSQL(sql);
    }

    @Test
    void setAutoCommit_shouldSetAutoCommit() throws SQLException {
        delegator.setAutoCommit(true);
        verify(connection).setAutoCommit(true);
    }

    @Test
    void getAutoCommit_shouldReturnAutoCommit() throws  SQLException {
        when(delegator.getAutoCommit()).thenReturn(true);
        assertTrue(delegator.getAutoCommit());
        verify(connection).getAutoCommit();
    }

    @Test
    void commit_shouldCommitTx() throws SQLException {
        delegator.commit();
        verify(connection).commit();
    }

    @Test
    void rollback_shouldRollbackTx() throws SQLException {
        delegator.rollback();
        verify(connection).rollback();
    }

    @Test
    void close_shouldCloseConnection() throws SQLException {
        delegator.close();
        verify(connection).close();
    }

    @Test
    void isClosed_shouldReturnBoolean() throws SQLException {
        when(delegator.isClosed()).thenReturn(true);
        assertTrue(delegator.isClosed());
        verify(connection).isClosed();
    }

    @Test
    void getMetadata_shouldReturnMetaData() throws SQLException {
        when(delegator.getMetaData()).thenReturn(mock(DatabaseMetaData.class));
        assertNotNull(delegator.getMetaData());
        verify(connection).getMetaData();
    }

    @Test
    void setReadOnly_shouldSetRadOnly() throws SQLException {
        delegator.setReadOnly(true);
        verify(connection).setReadOnly(true);
    }

    @Test
    void isReadOnly_shouldReturnBoolean() throws SQLException {
        when(delegator.isReadOnly()).thenReturn(true);
        assertTrue(delegator.isReadOnly());
        verify(connection).isReadOnly();
    }

    @Test
    void setCatalog_shouldSetCatalog() throws SQLException {
        delegator.setCatalog(testString);
        verify(connection).setCatalog(testString);
    }

    @Test
    void getCatalog_shouldReturnString() throws  SQLException {
        when(delegator.getCatalog()).thenReturn(testString);
        assertEquals(testString, delegator.getCatalog());
        verify(connection).getCatalog();
    }

    @Test
    void setTransactionIsolation_shouldSetTransactionIsolation() throws SQLException {
        delegator.setTransactionIsolation(Connection.TRANSACTION_READ_COMMITTED);
        verify(connection).setTransactionIsolation(Connection.TRANSACTION_READ_COMMITTED);
    }

    @Test
    void getTransactionIsolation_shouldReturnInt() throws SQLException {
        when(delegator.getTransactionIsolation()).thenReturn(Connection.TRANSACTION_READ_COMMITTED);
        assertEquals(Connection.TRANSACTION_READ_COMMITTED, delegator.getTransactionIsolation());
        verify(connection).getTransactionIsolation();
    }

    @Test
    void getWarnings_shouldReturnSQLWarning() throws  SQLException {
        when(delegator.getWarnings()).thenReturn(mock(SQLWarning.class));
        assertNotNull(delegator.getWarnings());
        verify(connection).getWarnings();
    }

    @Test
    void clearWarnings_shouldClearWarnings() throws SQLException {
        delegator.clearWarnings();
        verify(connection).clearWarnings();
    }

    @Test
    void createStatement_shouldCreateStatement_whenResultSetTypeAndResultSetConcurrency() throws SQLException {
        when(delegator.createStatement(ResultSet.TYPE_FORWARD_ONLY, ResultSet.CONCUR_READ_ONLY)).thenReturn(mock(Statement.class));
        assertNotNull(delegator.createStatement(ResultSet.TYPE_FORWARD_ONLY, ResultSet.CONCUR_READ_ONLY));
        verify(connection).createStatement(ResultSet.TYPE_FORWARD_ONLY, ResultSet.CONCUR_READ_ONLY);
    }

    @Test
    void prepareStatement_shouldCreatePreparedStatement_whenSqlAndResultSetTypeAndResultSetConcurrency() throws SQLException {
        when(delegator.prepareStatement(sql, ResultSet.TYPE_FORWARD_ONLY, ResultSet.CONCUR_READ_ONLY)).thenReturn(mock(PreparedStatement.class));
        assertNotNull(delegator.prepareStatement(sql, ResultSet.TYPE_FORWARD_ONLY, ResultSet.CONCUR_READ_ONLY));
        verify(connection).prepareStatement(sql, ResultSet.TYPE_FORWARD_ONLY, ResultSet.CONCUR_READ_ONLY);
    }

    @Test
    void prepareCall_shouldCreateCallableStatement_whenSqlAndResultSetTypeAndResultSetConcurrency() throws SQLException {
        when(delegator.prepareCall(sql, ResultSet.TYPE_FORWARD_ONLY, ResultSet.CONCUR_READ_ONLY)).thenReturn(mock(CallableStatement.class));
        assertNotNull(delegator.prepareCall(sql, ResultSet.TYPE_FORWARD_ONLY, ResultSet.CONCUR_READ_ONLY));
        verify(connection).prepareCall(sql, ResultSet.TYPE_FORWARD_ONLY, ResultSet.CONCUR_READ_ONLY);
    }

    @Test
    @SuppressWarnings("unchecked")
    void getTypeMap_shouldReturnMap() throws SQLException {
        when(delegator.getTypeMap()).thenReturn((Map<String, Class<?>>) mock(Map.class));
        delegator.getTypeMap();
        verify(connection).getTypeMap();
    }

    @Test
    @SuppressWarnings("unchecked")
    void setTypeMap_shouldSetTypeMap() throws SQLException {
        Map<String, Class<?>> map = (Map<String, Class<?>>) mock(Map.class);
        delegator.setTypeMap(map);
        verify(connection).setTypeMap(map);
    }

    @Test
    void setHoldability_shouldSetHoldability() throws SQLException {
        delegator.setHoldability(ResultSet.CLOSE_CURSORS_AT_COMMIT);
        verify(connection).setHoldability(ResultSet.CLOSE_CURSORS_AT_COMMIT);
    }

    @Test
    void getHoldability_shouldReturnInt() throws SQLException {
        when(delegator.getHoldability()).thenReturn(ResultSet.CLOSE_CURSORS_AT_COMMIT);
        assertEquals(ResultSet.CLOSE_CURSORS_AT_COMMIT, delegator.getHoldability());
        verify(connection).getHoldability();
    }

    @Test
    void setSavepoint_shouldReturnSavepoint() throws SQLException {
        when(delegator.setSavepoint()).thenReturn(mock(Savepoint.class));
        assertNotNull(delegator.setSavepoint());
        verify(connection).setSavepoint();
    }

    @Test
    void setSavepoint_shouldReturnSavepoint_whenString() throws SQLException {
        when(delegator.setSavepoint(testString)).thenReturn(mock(Savepoint.class));
        assertNotNull(delegator.setSavepoint(testString));
        verify(connection).setSavepoint(testString);
    }

    @Test
    void rollback_shouldRollBack_whenSavepoint() throws SQLException {
        Savepoint sp = mock(Savepoint.class);
        delegator.rollback(sp);
        verify(connection).rollback(sp);
    }

    @Test
    void releaseSavepoint_shouldReleaseSavepoint_whenSavepoint() throws SQLException {
        Savepoint sp = mock(Savepoint.class);
        delegator.releaseSavepoint(sp);
        verify(connection).releaseSavepoint(sp);
    }

    @Test
    void createStatement_shouldReturnStatement_whenResultSetTypeAndResultSetConcurrencyAndResultSetHoldability() throws SQLException {
        when(delegator.createStatement(ResultSet.TYPE_FORWARD_ONLY, ResultSet.CONCUR_READ_ONLY, ResultSet.HOLD_CURSORS_OVER_COMMIT)).thenReturn(mock(Statement.class));
        assertNotNull(delegator.createStatement(ResultSet.TYPE_FORWARD_ONLY, ResultSet.CONCUR_READ_ONLY, ResultSet.HOLD_CURSORS_OVER_COMMIT));
        verify(connection).createStatement(ResultSet.TYPE_FORWARD_ONLY, ResultSet.CONCUR_READ_ONLY, ResultSet.HOLD_CURSORS_OVER_COMMIT);
    }

    @Test
    void prepareStatement_shouldReturnPreparedStatement_whenSqlAndResultSetTypeAndResultSetConcurrencyAndResultSetHoldability() throws SQLException {
        when(delegator.prepareStatement(sql, ResultSet.TYPE_FORWARD_ONLY, ResultSet.CONCUR_READ_ONLY, ResultSet.HOLD_CURSORS_OVER_COMMIT)).thenReturn(mock(PreparedStatement.class));
        assertNotNull(delegator.prepareStatement(sql, ResultSet.TYPE_FORWARD_ONLY, ResultSet.CONCUR_READ_ONLY, ResultSet.HOLD_CURSORS_OVER_COMMIT));
        verify(connection).prepareStatement(sql, ResultSet.TYPE_FORWARD_ONLY, ResultSet.CONCUR_READ_ONLY, ResultSet.HOLD_CURSORS_OVER_COMMIT);
    }

    @Test
    void prepareCall_shouldReturnCallableStatement_whenSqlAndResultSetTypeAndResultSetConcurrencyAndResultSetHoldability() throws SQLException {
        when(delegator.prepareCall(sql, ResultSet.TYPE_FORWARD_ONLY, ResultSet.CONCUR_READ_ONLY, ResultSet.HOLD_CURSORS_OVER_COMMIT)).thenReturn(mock(CallableStatement.class));
        assertNotNull(delegator.prepareCall(sql, ResultSet.TYPE_FORWARD_ONLY, ResultSet.CONCUR_READ_ONLY, ResultSet.HOLD_CURSORS_OVER_COMMIT));
        verify(connection).prepareCall(sql, ResultSet.TYPE_FORWARD_ONLY, ResultSet.CONCUR_READ_ONLY, ResultSet.HOLD_CURSORS_OVER_COMMIT);
    }

    @Test
    void prepareStatement_shouldReturnPreparedStatement_whenSqlAndAutoGeneratedKeys() throws SQLException {
        when(delegator.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)).thenReturn(mock(PreparedStatement.class));
        assertNotNull(delegator.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS));
        verify(connection).prepareStatement(sql, Statement.RETURN_GENERATED_KEYS);
    }

    @Test
    void prepareStatement_shouldReturnPreparedStatement_whenSqlAndColumnIndexes() throws SQLException {
        int[] idxs = new int[] {testInt, testInt, testInt};
        when(delegator.prepareStatement(sql, idxs)).thenReturn(mock(PreparedStatement.class));
        assertNotNull(delegator.prepareStatement(sql, idxs));
        verify(connection).prepareStatement(sql, idxs);
    }

    @Test
    void prepareStatement_shouldReturnPreparedStatement_whenSqlAndColumnNames() throws SQLException {
        String[] names = new String[] {testString};
        when(delegator.prepareStatement(sql, names)).thenReturn(mock(PreparedStatement.class));
        assertNotNull(delegator.prepareStatement(sql, names));
        verify(connection).prepareStatement(sql, names);
    }

    @Test
    void createClob_shouldReturnClob() throws SQLException {
        when(delegator.createClob()).thenReturn(mock(Clob.class));
        assertNotNull(delegator.createClob());
        verify(connection).createClob();
    }

    @Test
    void createBlob_shouldReturnBlob() throws SQLException {
        when(delegator.createBlob()).thenReturn(mock(Blob.class));
        assertNotNull(delegator.createBlob());
        verify(connection).createBlob();
    }

    @Test
    void createNClob_shouldReturnNClob() throws SQLException {
        when(delegator.createNClob()).thenReturn(mock(NClob.class));
        assertNotNull(delegator.createNClob());
        verify(connection).createNClob();
    }

    @Test
    void createSQLXML_shouldReturnSQLXML() throws SQLException {
        when(delegator.createSQLXML()).thenReturn(mock(SQLXML.class));
        assertNotNull(delegator.createSQLXML());
        verify(connection).createSQLXML();
    }

    @Test
    void isValid_shouldReturnTrue() throws SQLException {
        when(delegator.isValid(testInt)).thenReturn(true);
        assertTrue(delegator.isValid(testInt));
        verify(connection).isValid(testInt);
    }

    @Test
    void setClientInfo_shouldSetClientInfo_whenNameAndValue() throws SQLClientInfoException {
        delegator.setClientInfo(testString, testString);
        verify(connection).setClientInfo(testString, testString);
    }

    @Test
    void setClientInfo_shouldSetClientInfo_whenProperties() throws SQLClientInfoException {
        Properties props = mock(Properties.class);
        delegator.setClientInfo(props);
        verify(connection).setClientInfo(props);
    }

    @Test
    void getClientInfo_shouldReturnString_whenName() throws SQLException {
        when(delegator.getClientInfo(testString)).thenReturn(testString);
        assertEquals(testString, delegator.getClientInfo(testString));
        verify(connection).getClientInfo(testString);
    }

    @Test
    void getClientInfo_shouldReturnProperties() throws SQLException {
        when(delegator.getClientInfo()).thenReturn(mock(Properties.class));
        assertNotNull(delegator.getClientInfo());
        verify(connection).getClientInfo();
    }

    @Test
    void createArrayOf_shouldReturnArray_whenTypeNameAndElements() throws SQLException {
        Array arr = mock(Array.class);
        Object[] objArr = new Object[] {};
        when(delegator.createArrayOf(testString, objArr)).thenReturn(arr);
        assertNotNull(delegator.createArrayOf(testString, objArr));
        verify(connection).createArrayOf(testString, objArr);
    }

    @Test
    void createStruct_shouldReturnStruct_whenTypeNameAndAttributes() throws SQLException {
        Struct struct = mock(Struct.class);
        Object[] objArr = new Object[] {};
        when(delegator.createStruct(testString, objArr)).thenReturn(struct);
        assertNotNull(delegator.createStruct(testString, objArr));
        verify(connection).createStruct(testString, objArr);
    }

    @Test
    void setSchema_shouldSetSchema_whenName() throws SQLException {
        delegator.setSchema(testString);
        verify(connection).setSchema(testString);
    }

    @Test
    void getSchema_shouldReturnString()  throws SQLException {
        when(delegator.getSchema()).thenReturn(testString);
        assertEquals(testString, delegator.getSchema());
        verify(connection).getSchema();
    }

    @Test
    void abort_shouldAbort_whenExecutor() throws SQLException {
        Executor mockExecutor = mock(Executor.class);
        delegator.abort(mockExecutor);
        verify(connection).abort(mockExecutor);
    }

    @Test
    void setNetworkTimeout_shouldSetNetworkTimeout_whenExecutorAndMillis() throws SQLException {
        Executor mockExecutor = mock(Executor.class);
        delegator.setNetworkTimeout(mockExecutor, testInt);
        verify(connection).setNetworkTimeout(mockExecutor, testInt);
    }

    @Test
    void getNetworkTimeout_shouldReturnInt() throws SQLException {
        when(delegator.getNetworkTimeout()).thenReturn(testInt);
        assertEquals(testInt, delegator.getNetworkTimeout());
        verify(connection).getNetworkTimeout();
    }

    @Test
    void unwrap_shouldReturnT_whenClassType() throws SQLException {
        Child child = new Child();
        when(delegator.unwrap(Parent.class)).thenReturn(child);
        assertEquals(child, delegator.unwrap(Parent.class));
        verify(connection).unwrap(Parent.class);
    }

    @Test
    void isWrapperFor_shouldReturnTrue() throws SQLException {
        when(delegator.isWrapperFor(Parent.class)).thenReturn(true);
        assertTrue(delegator.isWrapperFor(Parent.class));
        verify(connection).isWrapperFor(Parent.class);
    }
}