package integration;

import com.fasterxml.jackson.databind.JsonNode;
import com.sdlc.pro.txboard.enums.IsolationLevel;
import com.sdlc.pro.txboard.enums.PropagationBehavior;
import com.sdlc.pro.txboard.enums.TransactionPhaseStatus;
import integration.config.TestTxBoardConfig;
import integration.entity.Order;
import integration.entity.Product;
import integration.entity.User;
import integration.repository.ProductRepository;
import integration.repository.UserRepository;
import integration.service.OrderService;
import integration.service.UserService;
import org.junit.jupiter.api.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.TestPropertySource;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.web.client.RestTemplate;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;

@Tag("integration")
@SpringBootTest(
        classes = {TestTxBoardConfig.class, WebDashboardIntegrationTest.WebTestConfig.class},
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT
)
@EntityScan(basePackages = "integration.entity")
@EnableJpaRepositories(basePackages = "integration.repository")
@TestPropertySource("classpath:application-test.properties")
public class WebDashboardIntegrationTest {

    private static String baseUrl;

    @Autowired
    private RestTemplate restTemplate;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private UserService userService;

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private OrderService orderService;

    @Autowired
    private PlatformTransactionManager transactionManager;

    @BeforeAll
    static void setup(@LocalServerPort int port) {
        baseUrl = "http://localhost:" + port;
    }

    @Test
    void testAlarmingThresholdEndpoint() {
        ResponseEntity<JsonNode> response = restTemplate.getForEntity(
                baseUrl + "/api/tx-board/config/alarming-threshold",
                JsonNode.class
        );

        assertThat(response.getStatusCode().is2xxSuccessful()).isTrue();
        assertThat(Objects.requireNonNull(response.getHeaders().getContentType()).includes(MediaType.APPLICATION_JSON)).isTrue();

        JsonNode jsonNode = response.getBody();

        assertThat(jsonNode).isNotNull();
        assertThat(jsonNode.get("transaction").asLong()).isEqualTo(500L);
        assertThat(jsonNode.get("connection").asLong()).isEqualTo(500L);
    }

    @Nested
    @TestInstance(TestInstance.Lifecycle.PER_CLASS)
    class PrePerformedTransactionLogDependentRestApiTest {

        @BeforeAll
        void performTransactions() throws InterruptedException {
            TransactionTemplate template = new TransactionTemplate(transactionManager);

            // save users
            Stream.of("Sunny", "Jamil", "Jessy")
                    .map(name -> new User(null, name))
                    .forEach(userRepository::save);

            Product samsungMonitor = productRepository.save(new Product(null, "SAMSUNG LED Monitor", 25000, 10));
            Product dellLaptop = productRepository.save(new Product(null, "Dell Laptop", 55000, 5));
            Product iPhone17 = productRepository.save(new Product(null, "I Phone 17 Pro MAX", 120000, 2));

            User user1 = userRepository.save(new User(null, "Abdul Rahim"));

            // perform transaction with required-new propagation
            template.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
            template.executeWithoutResult(status -> orderService.placeOrder(new Order(null, user1, dellLaptop, 1)));

            // perform transaction with supports propagation
            template.setPropagationBehavior(TransactionDefinition.PROPAGATION_SUPPORTS);
            template.executeWithoutResult(status -> orderService.placeOrder(new Order(null, user1, iPhone17, 2)));

            User user2 = userRepository.save(new User(null, "Abdul Karim"));

            orderService.placeOrder(new Order(null, user2, samsungMonitor, 1));

            // perform 1 rolled back transaction
            try {
                orderService.placeOrder(new Order(null, user2, iPhone17, 1));
            } catch (Exception ignore) {
            }

            // save user with slow transaction with different thread
            Thread thread = new Thread(() -> Stream.of("Mira", "Mim")
                    .map(name -> new User(null, name))
                    .forEach(userService::slowUserRegistration), "slow-thread");

            thread.start();
            thread.join();

            userService.registerUserWithAware(new User(null, "Shamim"));

            // perform 2 errored transaction
            template.setPropagationBehavior(TransactionDefinition.PROPAGATION_MANDATORY);
            for (String name : List.of("Halima", "Sharif")) {
                try {
                    template.executeWithoutResult(status -> userRepository.save(new User(null, name)));
                } catch (Exception ignore) {
                }
            }

            // perform 1 transaction with propagation NOT_SUPPORTED and isolation READ_COMMITTED
            template.setPropagationBehavior(TransactionDefinition.PROPAGATION_NOT_SUPPORTED);
            template.setIsolationLevel(TransactionDefinition.ISOLATION_READ_COMMITTED);
            template.executeWithoutResult(status -> productRepository.findById(samsungMonitor.getId()));

            // perform 1 transaction with propagation NEVER and isolation READ_UNCOMMITTED
            template.setPropagationBehavior(TransactionDefinition.PROPAGATION_NEVER);
            template.setIsolationLevel(TransactionDefinition.ISOLATION_READ_UNCOMMITTED);
            template.executeWithoutResult(status -> productRepository.findById(dellLaptop.getId()));

            // perform post transaction queries
            String ignore = restTemplate.getForObject(baseUrl + "/api/v1/users/" + user1.getId(), String.class);
        }

        @Test
        void testTransactionSummaryEndpoint() {
            ResponseEntity<JsonNode> response = restTemplate.getForEntity(
                    baseUrl + "/api/tx-board/tx-summary",
                    JsonNode.class
            );

            assertThat(response.getStatusCode().is2xxSuccessful()).isTrue();

            JsonNode summaryJson = response.getBody();
            assertThat(summaryJson).isNotNull();

            assertThat(summaryJson.get("committedCount").isIntegralNumber()).isTrue();
            assertThat(summaryJson.get("rolledBackCount").isIntegralNumber()).isTrue();
            assertThat(summaryJson.get("erroredCount").isIntegralNumber()).isTrue();
            assertThat(summaryJson.get("totalDuration").isIntegralNumber()).isTrue();
            assertThat(summaryJson.get("alarmingCount").isIntegralNumber()).isTrue();
            assertThat(summaryJson.get("connectionAcquisitionCount").isIntegralNumber()).isTrue();
            assertThat(summaryJson.get("totalConnectionOccupiedTime").isIntegralNumber()).isTrue();
            assertThat(summaryJson.get("alarmingConnectionCount").isIntegralNumber()).isTrue();
            assertThat(summaryJson.get("totalTransaction").isIntegralNumber()).isTrue();
            assertThat(summaryJson.get("averageDuration").isFloatingPointNumber()).isTrue();
            assertThat(summaryJson.get("averageConnectionOccupiedTime").isFloatingPointNumber()).isTrue();
        }

        @Test
        void testTransactionChartEndpoint() {
            ResponseEntity<JsonNode> response = restTemplate.getForEntity(
                    baseUrl + "/api/tx-board/tx-charts",
                    JsonNode.class
            );

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);

            JsonNode json = response.getBody();
            assertThat(json).isNotNull();
            assertThat(json.get("durationDistribution").isArray()).isTrue();

            JsonNode durationDist = json.get("durationDistribution");
            assertThat(durationDist.size()).isEqualTo(5);

            assertThat(durationDist.get(0).path("range").path("minMillis").asInt()).isEqualTo(0);
            assertThat(durationDist.get(0).path("range").path("maxMillis").asInt()).isEqualTo(100);

            assertThat(durationDist.get(1).path("range").path("minMillis").asInt()).isEqualTo(101);
            assertThat(durationDist.get(1).path("range").path("maxMillis").asInt()).isEqualTo(500);

            assertThat(durationDist.get(2).path("range").path("minMillis").asInt()).isEqualTo(501);
            assertThat(durationDist.get(2).path("range").path("maxMillis").asInt()).isEqualTo(1000);

            assertThat(durationDist.get(3).path("range").path("minMillis").asInt()).isEqualTo(1001);
            assertThat(durationDist.get(3).path("range").path("maxMillis").asInt()).isEqualTo(2000);

            assertThat(durationDist.get(4).path("range").path("minMillis").asInt()).isEqualTo(2001);
            assertThat(durationDist.get(4).path("range").path("maxMillis").asInt()).isEqualTo(5000);
        }

        @Test
        void testTransactionLogsEndpoint() {
            ResponseEntity<JsonNode> response = restTemplate.getForEntity(
                    baseUrl + "/api/tx-board/tx-logs",
                    JsonNode.class
            );

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);

            JsonNode json = response.getBody();
            assertThat(json).isNotNull();
            assertThat(json.get("content").isArray()).isTrue();
            assertThat(json.get("totalElements").isNumber()).isTrue();
            assertThat(json.get("size").isNumber()).isTrue();
            assertThat(json.get("page").isNumber()).isTrue();
            assertThat(json.get("totalPages").isNumber()).isTrue();
            assertThat(json.get("first").isBoolean()).isTrue();
            assertThat(json.get("last").isBoolean()).isTrue();

            assertThat(json.get("content").size()).isEqualTo(10);
            assertThat(json.get("totalElements").asLong()).isEqualTo(20);
            assertThat(json.get("page").asInt()).isEqualTo(0);
            assertThat(json.get("size").asInt()).isEqualTo(10);
            assertThat(json.get("totalPages").asInt()).isEqualTo(2);
            assertThat(json.get("first").asBoolean()).isEqualTo(true);
            assertThat(json.get("last").asBoolean()).isEqualTo(false);
        }

        record PageData(int page, int size, boolean first, boolean last, int contentSize) {
        }

        static Stream<Arguments> pageDataProvider() {
            return Stream.of(
                    Arguments.of(new PageData(0, 7, true, false, 7)),
                    Arguments.of(new PageData(1, 7, false, false, 7)),
                    Arguments.of(new PageData(2, 7, false, true, 6))
            );
        }

        @ParameterizedTest
        @MethodSource(value = "pageDataProvider")
        void testTransactionLogsEndpointWithPagination(PageData data) {
            JsonNode json = restTemplate.getForObject(
                    baseUrl + "/api/tx-board/tx-logs?page=%d&size=%d".formatted(data.page(), data.size()),
                    JsonNode.class
            );

            assertThat(json).isNotNull();
            assertThat(json.get("content").size()).isEqualTo(data.contentSize());
            assertThat(json.get("totalElements").asLong()).isEqualTo(20);
            assertThat(json.get("page").asInt()).isEqualTo(data.page());
            assertThat(json.get("size").asInt()).isEqualTo(data.size());
            assertThat(json.get("totalPages").asInt()).isEqualTo(3);
            assertThat(json.get("first").asBoolean()).isEqualTo(data.first());
            assertThat(json.get("last").asBoolean()).isEqualTo(data.last());
        }


        final Map<TransactionPhaseStatus, Long> txCountForStatus = Map.of(
                TransactionPhaseStatus.COMMITTED, 17L,
                TransactionPhaseStatus.ROLLED_BACK, 1L,
                TransactionPhaseStatus.ERRORED, 2L
        );

        @ParameterizedTest
        @EnumSource(TransactionPhaseStatus.class)
        void testTransactionLogsFilterByStatus(TransactionPhaseStatus status) {
            JsonNode response = restTemplate.getForObject(
                    baseUrl + "/api/tx-board/tx-logs?status=" + status,
                    JsonNode.class
            );

            assertThat(response).isNotNull();
            assertThat(response.get("totalElements").asLong()).isEqualTo(txCountForStatus.get(status));
        }

        final Map<PropagationBehavior, Long> txCountForPropagation = Map.of(
                PropagationBehavior.UNKNOWN, 0L,
                PropagationBehavior.REQUIRED, 14L,
                PropagationBehavior.REQUIRES_NEW, 1L,
                PropagationBehavior.NESTED, 0L,
                PropagationBehavior.MANDATORY, 2L,
                PropagationBehavior.SUPPORTS, 1L,
                PropagationBehavior.NOT_SUPPORTED, 1L,
                PropagationBehavior.NEVER, 1L
        );

        @ParameterizedTest
        @EnumSource(PropagationBehavior.class)
        void testTransactionLogsFilterByPropagation(PropagationBehavior propagation) {
            JsonNode response = restTemplate.getForObject(
                    baseUrl + "/api/tx-board/tx-logs?propagation=" + propagation,
                    JsonNode.class
            );

            assertThat(response).isNotNull();
            assertThat(response.get("totalElements").asLong()).isEqualTo(txCountForPropagation.get(propagation));
        }

        final Map<IsolationLevel, Long> txCountForIsolationLevel = Map.of(
                IsolationLevel.DEFAULT, 16L,
                IsolationLevel.READ_COMMITTED, 1L,
                IsolationLevel.READ_UNCOMMITTED, 1L,
                IsolationLevel.REPEATABLE_READ, 2L,
                IsolationLevel.SERIALIZABLE, 0L
        );

        @ParameterizedTest
        @EnumSource(IsolationLevel.class)
        void testTransactionLogsFilterByIsolationLevel(IsolationLevel isolation) {
            JsonNode response = restTemplate.getForObject(
                    baseUrl + "/api/tx-board/tx-logs?isolation=" + isolation,
                    JsonNode.class
            );

            assertThat(response).isNotNull();
            assertThat(response.get("totalElements").asLong()).isEqualTo(txCountForIsolationLevel.get(isolation));
        }


        final Map<Boolean, Long> txCountForDBConnection = Map.of(
                true, 18L,
                false, 2L
        );

        @ParameterizedTest
        @ValueSource(booleans = {true, false})
        void testTransactionLogsFilterByConnectionOriented(boolean connection) {
            JsonNode response = restTemplate.getForObject(
                    baseUrl + "/api/tx-board/tx-logs?connectionOriented=" + connection,
                    JsonNode.class
            );

            assertThat(response).isNotNull();
            assertThat(response.get("totalElements").asLong()).isEqualTo(txCountForDBConnection.get(connection));
        }

        final Map<String, Long> txCountForSearchValue = Map.of(
                "OrderService.placeOrder", 2L,
                "UserService.slowUserRegistration", 2L,
                "UserService.registerUserWithAware", 1L,
                "anonymous", 6L,
                "main", 17L,
                "slow-thread", 2L
        );

        @ParameterizedTest
        @ValueSource(strings = {
                "OrderService.placeOrder",
                "UserService.slowUserRegistration",
                "UserService.registerUserWithAware",
                "anonymous",
                "main",
                "slow-thread"
        })
        void testTransactionLogsFilterBySearchValue(String searchValue) {
            JsonNode response = restTemplate.getForObject(
                    baseUrl + "/api/tx-board/tx-logs?search=" + searchValue,
                    JsonNode.class
            );

            assertThat(response).isNotNull();
            assertThat(response.get("totalElements").asLong()).isEqualTo(txCountForSearchValue.get(searchValue));
        }

        @Test
        void testOSIVEffectForLazyQueries() {
            JsonNode txLogJson = restTemplate.getForObject(
                    baseUrl + "/api/tx-board/tx-logs?page=0&size=1&sort=startTime,desc",
                    JsonNode.class
            );

            assertThat(txLogJson).isNotNull();
            JsonNode lastTxJson = txLogJson.get("content").get(0);
            assertThat(lastTxJson.get("postTransactionQuires").isArray()).isTrue();
            assertThat(lastTxJson.get("postTransactionQuires").size()).isEqualTo(1);
        }
    }

    @Configuration
    static class WebTestConfig {
        @Bean
        public RestTemplate restTemplate() {
            return new RestTemplate();
        }
    }
}
