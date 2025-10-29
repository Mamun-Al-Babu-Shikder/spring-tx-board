package integration.service;

import integration.entity.OutboxMessage;
import integration.entity.User;
import integration.model.UserRegisterPayload;
import integration.repository.UserRepository;
import integration.util.Utils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
public class UserService {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private OutboxMessageService outboxMessageService;

    @Transactional(transactionManager = "jdbcTransactionManager")
    public void registerUserWithAware(User user) {
        jdbcTemplate.update("insert into users(name) values(?)", user.getName());

        OutboxMessage message = new OutboxMessage();
        message.setTopic("user.registration");

        UserRegisterPayload payload = new UserRegisterPayload(user.getName(), true, "User registration done successfully!");
        message.setPayload(integration.util.Utils.objectToJsonString(payload));
        outboxMessageService.saveMessageWithNestedPropagation(message);

        if (user.getName() == null || !StringUtils.hasText(user.getName())) {
            throw new IllegalArgumentException("Failed to register user due to invalid username");
        }
    }

    @Transactional
    public void slowUserRegistration(User user) {
        userRepository.save(user);
        // simulate delay
        Utils.sleep(550);
    }
}
