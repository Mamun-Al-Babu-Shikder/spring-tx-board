package com.sdlc.pro.txboard.proxy;

import com.sdlc.pro.txboard.delegator.AbstractDataSourceDelegator;
import com.sdlc.pro.txboard.listener.TransactionPhaseListener;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.SQLException;

public final class TrackingDataSourceProxy extends AbstractDataSourceDelegator {
    private final TransactionPhaseListener transactionPhaseListener;

    public TrackingDataSourceProxy(DataSource dataSource, TransactionPhaseListener transactionPhaseListener) {
        super(dataSource);
        this.transactionPhaseListener = transactionPhaseListener;
    }

    @Override
    public Connection getConnection() throws SQLException {
        return createProxyConnection(super.getConnection());
    }

    @Override
    public Connection getConnection(String username, String password) throws SQLException {
        return createProxyConnection(super.getConnection(username, password));
    }

    private Connection createProxyConnection(Connection connection) {
        Connection interceptedConnection = new TrackingConnectionProxy(connection, this.transactionPhaseListener);
        this.transactionPhaseListener.afterAcquiredConnection();
        return interceptedConnection;
    }
}
