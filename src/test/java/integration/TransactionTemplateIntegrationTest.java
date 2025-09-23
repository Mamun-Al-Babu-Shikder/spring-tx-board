package integration;

import com.sdlc.pro.txboard.enums.IsolationLevel;
import com.sdlc.pro.txboard.enums.PropagationBehavior;
import com.sdlc.pro.txboard.enums.TransactionPhaseStatus;
import com.sdlc.pro.txboard.model.TransactionEvent;
import com.sdlc.pro.txboard.model.TransactionLog;
import integration.config.TestTxBoardConfig;
import integration.entity.Order;
import integration.entity.Product;
import integration.entity.User;
import integration.listener.TestTransactionLogListener;
import integration.repository.OrderRepository;
import integration.repository.ProductRepository;
import integration.repository.UserRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.test.context.TestPropertySource;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@Tag("integration")
@SpringBootTest(classes = TestTxBoardConfig.class)
@EntityScan(basePackages = "integration.entity")
@EnableJpaRepositories(basePackages = "integration.repository")
@TestPropertySource("classpath:application-test.properties")
public class TransactionTemplateIntegrationTest {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private OrderRepository orderRepository;

    @Autowired
    private TransactionTemplate transactionTemplate;

    @Autowired
    private PlatformTransactionManager transactionManager;

    @Autowired
    private TestTransactionLogListener logListener;

    @AfterEach
    void cleanup() {
        logListener.clear();
    }

    @Test
    void testSimpleTransactionalOperation() {
        transactionTemplate.executeWithoutResult(status -> userRepository.save(new User(null, "Arafat Hassan")));

        assertEquals(1, logListener.getTotalTransactionLog());

        TransactionLog transactionLog = logListener.getTransactionLogByIndex(0);
        assertEquals("anonymous", transactionLog.getMethod());
        assertEquals(PropagationBehavior.REQUIRED, transactionLog.getPropagation());
        assertEquals(IsolationLevel.DEFAULT, transactionLog.getIsolation());
        assertEquals(TransactionPhaseStatus.COMMITTED, transactionLog.getStatus());

        assertTrue(transactionLog.getConnectionOriented());
        assertEquals(1, transactionLog.getConnectionSummary().acquisitionCount());
        assertTrue(transactionLog.getExecutedQuires().isEmpty());

        assertEquals(1, transactionLog.getChild().size());
        TransactionLog childTransactionLog = transactionLog.getChild().get(0);
        assertEquals("SimpleJpaRepository.save", childTransactionLog.getMethod());
        assertEquals(TransactionPhaseStatus.COMMITTED, childTransactionLog.getStatus());
        assertTrue(childTransactionLog.getChild().isEmpty());

        List<TransactionEvent> events = transactionLog.getEvents();
        assertFalse(events.isEmpty());
    }

    @Test
    void testCommittedTransaction() {
        User user = userRepository.save(new User(null, "Abdul Karim"));
        Product product = productRepository.save(new Product(null, "Dell Laptop", 55000.0, 5));

        Order order = new Order(null, user, product, 3);

        transactionTemplate.executeWithoutResult(status -> {
            if (order.getQuantity() > product.getStock()) {
                throw new IllegalStateException("Not enough stock for product " + product.getId());
            }
            orderRepository.save(order);
        });

        TransactionLog transactionLog = logListener.getLastTransactionLog();
        assertEquals("anonymous", transactionLog.getMethod());
        assertEquals(TransactionPhaseStatus.COMMITTED, transactionLog.getStatus());
    }

    @Test
    void testRolledBackTransaction() {
        User user = userRepository.save(new User(null, "Abdul Rahim"));
        Product product = productRepository.save(new Product(null, "HP Laptop", 75000.0, 1));

        Order order = new Order(null, user, product, 2);

        try {
            transactionTemplate.executeWithoutResult(status -> {
                if (order.getQuantity() > product.getStock()) {
                    throw new IllegalStateException("Not enough stock for product " + product.getId());
                }
                orderRepository.save(order);
            });
        } catch (Exception ignore) {
        }

        TransactionLog transactionLog = logListener.getLastTransactionLog();
        assertEquals("anonymous", transactionLog.getMethod());
        assertEquals(TransactionPhaseStatus.ROLLED_BACK, transactionLog.getStatus());
    }

    @Test
    void testErroredTransaction() {
        User user = new User(null, "Abdul Karim");
        TransactionTemplate template = new TransactionTemplate(transactionManager);
        template.setPropagationBehavior(TransactionDefinition.PROPAGATION_MANDATORY);

        try {
            template.executeWithoutResult(status->{
                userRepository.save(user);
            });
        } catch (Exception ignore) {
        }

        TransactionLog transactionLog = logListener.getLastTransactionLog();
        assertEquals(TransactionPhaseStatus.ERRORED, transactionLog.getStatus());
        assertFalse(transactionLog.getConnectionOriented());
    }
}
