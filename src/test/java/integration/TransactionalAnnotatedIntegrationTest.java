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
import integration.repository.OutboxMessageRepository;
import integration.repository.ProductRepository;
import integration.repository.UserRepository;
import integration.service.OrderService;
import integration.service.ProductService;
import integration.service.TestService;
import integration.service.UserService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.test.context.TestPropertySource;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@Tag("integration")
@SpringBootTest(classes = TestTxBoardConfig.class)
@EntityScan(basePackages = "integration.entity")
@EnableJpaRepositories(basePackages = "integration.repository")
@TestPropertySource("classpath:application-test.properties")
public class TransactionalAnnotatedIntegrationTest {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private OrderRepository orderRepository;

    @Autowired
    private OutboxMessageRepository outboxMessageRepository;

    @Autowired
    private OrderService orderService;

    @Autowired
    private ProductService productService;

    @Autowired
    private UserService userService;

    @Autowired
    private TestService testService;

    @Autowired
    private TestTransactionLogListener logListener;

    @AfterEach
    void cleanup() {
        orderRepository.deleteAll();
        productRepository.deleteAll();
        userRepository.deleteAll();
        outboxMessageRepository.deleteAll();
        logListener.clear();
    }

    @Test
    void testSimpleTransactionalOperation() {
        userRepository.save(new User(null, "Mira Jahan"));

        assertEquals(1, logListener.getTotalTransactionLog());

        TransactionLog transactionLog = logListener.getLastTransactionLog();
        assertEquals("SimpleJpaRepository.save", transactionLog.getMethod());
        assertEquals(PropagationBehavior.REQUIRED, transactionLog.getPropagation());
        assertEquals(IsolationLevel.DEFAULT, transactionLog.getIsolation());
        assertEquals(TransactionPhaseStatus.COMMITTED, transactionLog.getStatus());

        assertTrue(transactionLog.getConnectionOriented());
        assertEquals(1, transactionLog.getConnectionSummary().getAcquisitionCount());
        assertEquals(1, transactionLog.getTotalQueryCount());
        assertTrue(transactionLog.getChild().isEmpty());

        List<TransactionEvent> events = transactionLog.getEvents();
        assertEquals(TransactionEvent.Type.CONNECTION_ACQUIRED, events.get(0).getType());
        assertEquals(TransactionEvent.Type.TRANSACTION_START, events.get(1).getType());
        assertEquals(TransactionEvent.Type.CONNECTION_RELEASED, events.get(2).getType());
        assertEquals(TransactionEvent.Type.TRANSACTION_END, events.get(3).getType());
    }

    @Test
    void testCommittedTransaction() {
        User user = userRepository.save(new User(null, "Abdul Karim"));
        Product product = productRepository.save(new Product(null, "Dell Laptop", 55000.0, 5));

        Order order = new Order(null, user, product, 3);
        orderService.placeOrder(order);

        TransactionLog transactionLog = logListener.getLastTransactionLog();
        assertEquals("OrderService.placeOrder", transactionLog.getMethod());
        assertEquals(TransactionPhaseStatus.COMMITTED, transactionLog.getStatus());

        TransactionLog deductStockTxLog = transactionLog.getChild().get(0);
        assertEquals("ProductService.deductStock", deductStockTxLog.getMethod());
        assertEquals(TransactionPhaseStatus.COMMITTED, deductStockTxLog.getStatus());
    }

    @Test
    void testRolledBackTransaction() {
        User user = userRepository.save(new User(null, "Abdul Rahim"));
        Product product = productRepository.save(new Product(null, "HP Laptop", 75000.0, 1));

        Order order = new Order(null, user, product, 2);
        try {
            orderService.placeOrder(order);
        } catch (Exception ignore) {
        }

        TransactionLog transactionLog = logListener.getLastTransactionLog();
        assertEquals("OrderService.placeOrder", transactionLog.getMethod());
        assertEquals(TransactionPhaseStatus.ROLLED_BACK, transactionLog.getStatus());

        TransactionLog deductStockTxLog = transactionLog.getChild().get(0);
        assertEquals("ProductService.deductStock", deductStockTxLog.getMethod());
        assertEquals(TransactionPhaseStatus.ROLLED_BACK, deductStockTxLog.getStatus());
    }

    @Test
    void testErroredTransaction() {
        Product product = productRepository.save(new Product(null, "SAMSUNG Phone", 35000.0, 10));

        try {
            productService.deductStock(product.getId(), 1);
        } catch (Exception ignore) {
        }

        TransactionLog transactionLog = logListener.getLastTransactionLog();
        assertEquals("ProductService.deductStock", transactionLog.getMethod());
        assertEquals(TransactionPhaseStatus.ERRORED, transactionLog.getStatus());
        assertFalse(transactionLog.getConnectionOriented());

        List<TransactionEvent> events = transactionLog.getEvents();
        assertEquals(2, events.size());
        assertEquals(TransactionEvent.Type.TRANSACTION_START, events.get(0).getType());
        assertEquals(TransactionEvent.Type.TRANSACTION_END, events.get(1).getType());

        assertTrue(transactionLog.getChild().isEmpty());
    }

    @Test
    void testRequiredNewPropagationWithOuterCommittedTransaction() {
        User user = userRepository.save(new User(null, "Sadik Islam"));
        Product product = productRepository.save(new Product(null, "ASUS Laptop", 85000.0, 5));

        Order order = new Order(null, user, product, 1);
        orderService.placeOrderWithAwareUser(order);

        TransactionLog transactionLog = logListener.getLastTransactionLog();
        assertEquals("OrderService.placeOrderWithAwareUser", transactionLog.getMethod());
        assertEquals(PropagationBehavior.REQUIRED, transactionLog.getPropagation());
        assertEquals(TransactionPhaseStatus.COMMITTED, transactionLog.getStatus());
        assertEquals(2, transactionLog.getConnectionSummary().getAcquisitionCount());

        TransactionLog outboxTransactionLog = transactionLog.getChild().get(2);
        assertEquals("OutboxMessageService.saveMessageWithRequiresNewPropagation", outboxTransactionLog.getMethod());
        assertEquals(PropagationBehavior.REQUIRES_NEW, outboxTransactionLog.getPropagation());
        assertEquals(TransactionPhaseStatus.COMMITTED, outboxTransactionLog.getStatus());
    }

    @Test
    void testRequiredNewPropagationWithOuterRollbackTransaction() {
        User user = userRepository.save(new User(null, "Mim"));
        Product product = productRepository.save(new Product(null, "Lenovo Laptop", 65000.0, 1));

        Order order = new Order(null, user, product, 2);
        try {
            orderService.placeOrderWithAwareUser(order);
        } catch (Exception ignore) {
        }

        TransactionLog transactionLog = logListener.getLastTransactionLog();
        assertEquals("OrderService.placeOrderWithAwareUser", transactionLog.getMethod());
        assertEquals(PropagationBehavior.REQUIRED, transactionLog.getPropagation());
        assertEquals(TransactionPhaseStatus.ROLLED_BACK, transactionLog.getStatus());
        assertEquals(2, transactionLog.getConnectionSummary().getAcquisitionCount());

        TransactionLog outboxTransactionLog = transactionLog.getChild().get(1);
        assertEquals("OutboxMessageService.saveMessageWithRequiresNewPropagation", outboxTransactionLog.getMethod());
        assertEquals(PropagationBehavior.REQUIRES_NEW, outboxTransactionLog.getPropagation());
        assertEquals(TransactionPhaseStatus.COMMITTED, outboxTransactionLog.getStatus());
    }

    @Test
    void testNestedPropagationWithOuterCommittedTransaction() {
        User user = userRepository.save(new User(null, "Maliha"));
        Product product = productRepository.save(new Product(null, "iPhone 17", 150000.0, 10));
        Order order = new Order(null, user, product, 2);

        orderService.placeOrderWithNestedUserAwareness(order);

        TransactionLog transactionLog = logListener.getLastTransactionLog();
        assertEquals("OrderService.placeOrderWithNestedUserAwareness", transactionLog.getMethod());
        assertEquals(PropagationBehavior.REQUIRED, transactionLog.getPropagation());
        assertEquals(TransactionPhaseStatus.COMMITTED, transactionLog.getStatus());
        assertEquals(1, transactionLog.getConnectionSummary().getAcquisitionCount());

        TransactionLog outboxTransactionLog = transactionLog.getChild().get(0);
        assertEquals("OutboxMessageService.saveMessageWithNestedPropagation", outboxTransactionLog.getMethod());
        assertEquals(PropagationBehavior.NESTED, outboxTransactionLog.getPropagation());
        assertEquals(TransactionPhaseStatus.COMMITTED, outboxTransactionLog.getStatus());
    }

    @Test
    void testNestedPropagationWithOuterRollbackTransaction() {
        User user = new User(null, "");

        try {
            userService.registerUserWithAware(user);
        } catch (Exception ignore) {
        }

        TransactionLog transactionLog = logListener.getLastTransactionLog();
        assertEquals(PropagationBehavior.REQUIRED, transactionLog.getPropagation());
        assertEquals("UserService.registerUserWithAware", transactionLog.getMethod());
        assertEquals(TransactionPhaseStatus.ROLLED_BACK, transactionLog.getStatus());
        assertEquals(1, transactionLog.getConnectionSummary().getAcquisitionCount());

        TransactionLog outboxTransactionLog = transactionLog.getChild().get(0);
        assertEquals("OutboxMessageService.saveMessageWithNestedPropagation", outboxTransactionLog.getMethod());
        assertEquals(PropagationBehavior.NESTED, outboxTransactionLog.getPropagation());
        assertEquals(TransactionPhaseStatus.COMMITTED, outboxTransactionLog.getStatus());
    }

    @Test
    void testLogRunningTransaction() {
        User user = new User(null, "Murad");
        userService.slowUserRegistration(user);

        TransactionLog transactionLog = logListener.getLastTransactionLog();
        assertTrue(transactionLog.isAlarmingTransaction());
        assertTrue(transactionLog.getDuration() > 500);
    }

    @Test
    void testConnectionLessTransaction() {
        testService.performConnectionLessTransaction();

        TransactionLog transactionLog = logListener.getLastTransactionLog();
        assertFalse(transactionLog.getConnectionOriented());
        assertEquals(0, transactionLog.getConnectionSummary().getAcquisitionCount());

        List<TransactionEvent> events = transactionLog.getEvents();
        assertEquals(2, events.size());
        assertEquals(TransactionEvent.Type.TRANSACTION_START, events.get(0).getType());
        assertEquals(TransactionEvent.Type.TRANSACTION_END, events.get(1).getType());
    }
}
