package integration.service;

import integration.entity.Order;
import integration.entity.OutboxMessage;
import integration.model.OrderStatusPayload;
import integration.repository.OrderRepository;
import integration.util.Utils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.sql.PreparedStatement;

@Service
public class OrderService {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private OrderRepository orderRepository;

    @Autowired
    private ProductService productService;

    @Autowired
    private OutboxMessageService outboxMessageService;


    @Transactional
    public void placeOrder(Order order) {
        productService.deductStock(order.getProduct().getId(), order.getQuantity());
        orderRepository.save(order);
    }

    @Transactional
    public void placeOrderWithAwareUser(Order order) {
        OutboxMessage outboxMessage = new OutboxMessage();
        outboxMessage.setTopic("order.product");

        try {
            productService.deductStock(order.getProduct().getId(), order.getQuantity());
            Order savedOrder = orderRepository.save(order);
            OrderStatusPayload successPayload = new OrderStatusPayload(savedOrder.getId(), order.getUser().getId(), true, "Order placed successfully!");
            outboxMessage.setPayload(Utils.objectToJsonString(successPayload));
            outboxMessageService.saveMessageWithRequiresNewPropagation(outboxMessage);
        } catch (Exception ex) {
            OrderStatusPayload failedPayload = new OrderStatusPayload(null, order.getUser().getId(), false, "Failed to placed order!");
            outboxMessage.setPayload(Utils.objectToJsonString(failedPayload));
            outboxMessageService.saveMessageWithRequiresNewPropagation(outboxMessage);
            throw new RuntimeException("Failed to place the order");
        }
    }


    @Transactional(transactionManager = "jdbcTransactionManager")
    public void placeOrderWithNestedUserAwareness(Order order) {
        OutboxMessage outboxMessage = new OutboxMessage();
        outboxMessage.setTopic("order.product");

        try {
            KeyHolder keyHolder = new GeneratedKeyHolder();
            jdbcTemplate.update(con -> {
                PreparedStatement statement = con.prepareStatement("insert into orders(user_id, product_id, quantity) values(?, ?, ?)", PreparedStatement.RETURN_GENERATED_KEYS);
                statement.setLong(1, order.getUser().getId());
                statement.setLong(2, order.getProduct().getId());
                statement.setInt(3, order.getQuantity());
                return statement;
            }, keyHolder);

            Long orderId = (Long) keyHolder.getKey();
            OrderStatusPayload successPayload = new OrderStatusPayload(orderId, order.getUser().getId(), true, "Order placed successfully!");
            outboxMessage.setPayload(Utils.objectToJsonString(successPayload));
            outboxMessageService.saveMessageWithNestedPropagation(outboxMessage);
        } catch (Exception ex) {
            OrderStatusPayload failedPayload = new OrderStatusPayload(null, order.getUser().getId(), false, "Failed to placed order!");
            outboxMessage.setPayload(Utils.objectToJsonString(failedPayload));
            outboxMessageService.saveMessageWithNestedPropagation(outboxMessage);
            throw new RuntimeException("Failed to place the order");
        }
    }
}
