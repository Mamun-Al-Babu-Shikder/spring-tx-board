package integration.repository;

import integration.entity.OutboxMessage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface OutboxMessageRepository extends JpaRepository<OutboxMessage, Long> {
}
