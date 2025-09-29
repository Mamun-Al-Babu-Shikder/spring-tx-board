package com.sdlc.pro.txboard.listener;

import com.sdlc.pro.txboard.config.TxBoardProperties;
import com.sdlc.pro.txboard.model.TransactionLog;
import com.sdlc.pro.txboard.repository.InMemoryTransactionLogRepository;
import com.sdlc.pro.txboard.repository.TransactionLogRepository;
import org.junit.jupiter.api.Test;
import org.springframework.transaction.support.DefaultTransactionDefinition;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

public class NPlusOneDetectionTest {

    @Test
    void shouldDetectNPlusOneQueries() {
        TxBoardProperties props = new TxBoardProperties();
        props.setLogType(TxBoardProperties.LogType.DETAILS);

        TransactionLogRepository repo = new InMemoryTransactionLogRepository(props);
        TransactionLogListener persistenceListener = new TransactionLogPersistenceListener(repo);

        TransactionPhaseListenerImpl listener = new TransactionPhaseListenerImpl(List.of(persistenceListener), props);

        // Create a transaction definition with a short method-like name
        DefaultTransactionDefinition def = new DefaultTransactionDefinition();
        def.setName("com.example.UserService.loadUsers");

        // Begin transaction
        listener.beforeBegin(def);
        listener.afterBegin(null);

        // Acquire connection
        listener.afterAcquiredConnection();

        // Main query
        listener.executedQuery("select u.id, u.name from users u where u.active = true");

        // Simulate N child queries: select posts by user_id (different literal each time)
        int N = 8;
        for (int i = 1; i <= N; i++) {
            listener.executedQuery("select p.id, p.title from posts p where p.user_id = " + i);
        }

        // End transaction
        listener.afterCommit();
        listener.afterCloseConnection();

        List<TransactionLog> logs = repo.findAll();
        assertThat(logs).isNotEmpty();
        TransactionLog last = logs.get(logs.size() - 1);

        assertThat(last.getTotalQueryCount()).isEqualTo(1 + N);
        assertThat(last.isNPlusOneDetected()).isTrue();
    }
}
