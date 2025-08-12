package com.sdlc.pro.txboard.proxy;

import com.sdlc.pro.txboard.delegator.AbstractConnectionDelegator;
import com.sdlc.pro.txboard.listener.TransactionPhaseListener;

import java.sql.CallableStatement;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;

public final class TrackingConnectionProxy extends AbstractConnectionDelegator {
    private final TransactionPhaseListener transactionPhaseListener;

    public TrackingConnectionProxy(Connection connection, TransactionPhaseListener transactionPhaseListener) {
        super(connection);
        this.transactionPhaseListener = transactionPhaseListener;
    }

    @Override
    public PreparedStatement prepareStatement(String sql) throws SQLException {
        this.transactionPhaseListener.executedQuery(sql);
        return super.prepareStatement(sql);
    }

    @Override
    public CallableStatement prepareCall(String sql) throws SQLException {
        this.transactionPhaseListener.executedQuery(sql);
        return super.prepareCall(sql);
    }

    @Override
    public String nativeSQL(String sql) throws SQLException {
        this.transactionPhaseListener.executedQuery(sql);
        return super.nativeSQL(sql);
    }

    @Override
    public PreparedStatement prepareStatement(String sql, int resultSetType, int resultSetConcurrency) throws SQLException {
        this.transactionPhaseListener.executedQuery(sql);
        return super.prepareStatement(sql, resultSetType, resultSetConcurrency);
    }

    @Override
    public void close() throws SQLException {
        super.close();
        this.transactionPhaseListener.afterCloseConnection();
    }
}
