package integration.service;

import integration.entity.OutboxMessage;
import integration.repository.OutboxMessageRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service
public class OutboxMessageService {

    @Autowired
    private OutboxMessageRepository outboxMessageRepository;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void saveMessageWithRequiresNewPropagation(OutboxMessage message) {
        outboxMessageRepository.save(message);
    }

    @Transactional(transactionManager = "jdbcTransactionManager", propagation = Propagation.NESTED)
    public void saveMessageWithNestedPropagation(OutboxMessage message) {
        String SQL = "insert into outbox_messages(topic, payload, status, created_at) values (?, ?, ?, ?)";
        jdbcTemplate.update(SQL, message.getTopic(), message.getPayload(), message.getStatus().name(), message.getCreatedAt());
    }
}
