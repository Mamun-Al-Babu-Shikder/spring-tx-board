package integration.listener;

import com.sdlc.pro.txboard.listener.TransactionLogListener;
import com.sdlc.pro.txboard.model.TransactionLog;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@Component
public class TestTransactionLogListener implements TransactionLogListener {
    private final List<TransactionLog> txLogs = Collections.synchronizedList(new ArrayList<>());

    @Override
    public void listen(TransactionLog transactionLog) {
        this.txLogs.add(transactionLog);
    }

    public TransactionLog getTransactionLogByIndex(int index) {
        return this.txLogs.get(index);
    }

    public TransactionLog getLastTransactionLog() {
        return this.txLogs.get(getTotalTransactionLog() - 1);
    }

    public int getTotalTransactionLog() {
        return this.txLogs.size();
    }

    public void clear() {
        this.txLogs.clear();
    }
}
