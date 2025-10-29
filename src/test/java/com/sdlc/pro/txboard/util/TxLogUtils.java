package com.sdlc.pro.txboard.util;

import com.sdlc.pro.txboard.enums.IsolationLevel;
import com.sdlc.pro.txboard.enums.PropagationBehavior;
import com.sdlc.pro.txboard.enums.TransactionPhaseStatus;
import com.sdlc.pro.txboard.model.ConnectionSummary;
import com.sdlc.pro.txboard.model.TransactionEvent;
import com.sdlc.pro.txboard.model.TransactionLog;

import java.time.Duration;
import java.time.Instant;
import java.util.*;

public final class TxLogUtils {
    private static final long TX_ALARMING_THRESHOLD = 1000L;
    private static final long CONNECTION_ALARMING_THRESHOLD = 1000L;

    public static List<TransactionLog> createTestTransactionLogs() {
        Instant baseTime = Instant.parse("2025-08-20T10:45:00Z");

        // Order Processing
        Instant orderStart = baseTime.plusMillis(30);
        TransactionLog inventoryCheck = new TransactionLog(
                null, "InventoryService.checkAvailability",
                PropagationBehavior.REQUIRED, IsolationLevel.READ_COMMITTED,
                orderStart.plusMillis(50), orderStart.plusMillis(120), null,
                TransactionPhaseStatus.COMMITTED, "order-processor-1",
                Arrays.asList(
                        "SELECT quantity FROM inventory WHERE product_id = ? AND warehouse_id = ?",
                        "UPDATE inventory SET reserved_quantity = reserved_quantity + ? WHERE product_id = ?"
                ),
                Collections.emptyList(),
                null,
                TX_ALARMING_THRESHOLD
        );

        TransactionLog paymentProcessing = new TransactionLog(
                null, "PaymentService.processPayment",
                PropagationBehavior.REQUIRED, IsolationLevel.SERIALIZABLE,
                orderStart.plusMillis(200), orderStart.plusMillis(800),
                null,
                TransactionPhaseStatus.COMMITTED, "order-processor-1",
                Arrays.asList(
                        "INSERT INTO payments (order_id, amount, gateway, status) VALUES (?, ?, 'STRIPE', 'PROCESSING')",
                        "UPDATE payments SET status = 'COMPLETED', transaction_id = ? WHERE id = ?",
                        "INSERT INTO payment_audit_log (payment_id, action, timestamp) VALUES (?, 'CHARGED', NOW())"
                ),
                Collections.emptyList(),
                null,
                TX_ALARMING_THRESHOLD
        );

        List<TransactionEvent> orderEvents = Arrays.asList(
                new TransactionEvent(TransactionEvent.Type.CONNECTION_ACQUIRED, orderStart, "Connection Acquired [1]"),
                new TransactionEvent(TransactionEvent.Type.TRANSACTION_START, orderStart, "Transaction Start [OrderService.createOrder]"),
                new TransactionEvent(TransactionEvent.Type.TRANSACTION_START, orderStart.plusMillis(50), "Transaction Start [InventoryService.checkAvailability]"),
                new TransactionEvent(TransactionEvent.Type.TRANSACTION_END, orderStart.plusMillis(120), "Transaction End [InventoryService.checkAvailability]"),
                new TransactionEvent(TransactionEvent.Type.TRANSACTION_START, orderStart.plusMillis(200), "Transaction Start [PaymentService.processPayment]"),
                new TransactionEvent(TransactionEvent.Type.TRANSACTION_END, orderStart.plusMillis(800), "Transaction End [PaymentService.processPayment]"),
                new TransactionEvent(TransactionEvent.Type.TRANSACTION_END, orderStart.plusMillis(950), "Transaction End [OrderService.createOrder]"),
                new TransactionEvent(TransactionEvent.Type.CONNECTION_RELEASED, orderStart.plusMillis(950), "Connection Released [1]")
        );

        ConnectionSummary orderConnectionSummary = fromEvents(orderEvents);
        TransactionLog orderProcessing = new TransactionLog(
                101, "OrderService.createOrder",
                PropagationBehavior.REQUIRED, IsolationLevel.READ_COMMITTED,
                orderStart, orderStart.plusMillis(950), orderConnectionSummary,
                TransactionPhaseStatus.COMMITTED, "order-processor-1",
                Arrays.asList(
                        "INSERT INTO orders (customer_id, total_amount, status, created_at) VALUES (?, ?, 'PENDING', NOW())",
                        "INSERT INTO order_items (order_id, product_id, quantity, price) VALUES (?, ?, ?, ?)",
                        "UPDATE orders SET status = 'CONFIRMED' WHERE id = ?"
                ),
                Arrays.asList(inventoryCheck, paymentProcessing),
                orderEvents, TX_ALARMING_THRESHOLD
        );

        // User Registration
        Instant userRegStart = baseTime.plusSeconds(60);
        TransactionLog emailVerification = new TransactionLog(
                null, "EmailService.sendVerificationEmail",
                PropagationBehavior.MANDATORY, IsolationLevel.READ_COMMITTED,
                userRegStart.plusMillis(100), userRegStart.plusMillis(350),
                null,
                TransactionPhaseStatus.COMMITTED,
                "auth-service-2",
                Arrays.asList(
                        "INSERT INTO email_queue (recipient, subject, body, status) VALUES (?, 'Verify Your Account', ?, 'QUEUED')",
                        "UPDATE email_queue SET status = 'SENT', sent_at = NOW() WHERE id = ?"
                ),
                Collections.emptyList(),
                null,
                TX_ALARMING_THRESHOLD
        );

        List<TransactionEvent> userRegEvents = Arrays.asList(
                new TransactionEvent(TransactionEvent.Type.CONNECTION_ACQUIRED, userRegStart, "Connection Acquired [1]"),
                new TransactionEvent(TransactionEvent.Type.TRANSACTION_START, userRegStart, "Transaction Start [UserService.registerUser]"),
                new TransactionEvent(TransactionEvent.Type.TRANSACTION_START, userRegStart.plusMillis(100), "Transaction Start [EmailService.sendVerificationEmail]"),
                new TransactionEvent(TransactionEvent.Type.TRANSACTION_END, userRegStart.plusMillis(350), "Transaction End [EmailService.sendVerificationEmail]"),
                new TransactionEvent(TransactionEvent.Type.TRANSACTION_END, userRegStart.plusMillis(450), "Transaction End [UserService.registerUser]"),
                new TransactionEvent(TransactionEvent.Type.CONNECTION_RELEASED, userRegStart.plusMillis(450), "Connection Released [1]")
        );

        ConnectionSummary userRegConnSummary = fromEvents(userRegEvents);
        TransactionLog userRegistration = new TransactionLog(
                102, "UserService.registerUser",
                PropagationBehavior.REQUIRED, IsolationLevel.READ_COMMITTED,
                userRegStart, userRegStart.plusMillis(450), userRegConnSummary,
                TransactionPhaseStatus.ROLLED_BACK, "auth-service-2",
                Arrays.asList(
                        "SELECT COUNT(*) FROM users WHERE email = ?",
                        "INSERT INTO users (username, email, password_hash, status, created_at) VALUES (?, ?, ?, 'PENDING_VERIFICATION', NOW())",
                        "INSERT INTO user_profiles (user_id, first_name, last_name) VALUES (?, ?, ?)",
                        "INSERT INTO verification_tokens (user_id, token, expires_at) VALUES (?, ?, ?)"
                ),
                Collections.singletonList(emailVerification),
                userRegEvents,
                TX_ALARMING_THRESHOLD
        );

        // Report Generation (long-running)
        Instant reportStart = baseTime.plusSeconds(90);
        List<TransactionEvent> reportEvents = Arrays.asList(
                new TransactionEvent(TransactionEvent.Type.CONNECTION_ACQUIRED, reportStart, "Connection Acquired [1]"),
                new TransactionEvent(TransactionEvent.Type.TRANSACTION_START, reportStart, "Transaction Start [ReportService.generateMonthlyReport]"),
                new TransactionEvent(TransactionEvent.Type.TRANSACTION_END, reportStart.plusMillis(3200), "Transaction End [ReportService.generateMonthlyReport]"),
                new TransactionEvent(TransactionEvent.Type.CONNECTION_RELEASED, reportStart.plusMillis(3200), "Connection Released [1]")
        );

        ConnectionSummary reportConnSummary = fromEvents(reportEvents);
        TransactionLog reportGeneration = new TransactionLog(
                208, "ReportService.generateMonthlyReport",
                PropagationBehavior.REQUIRES_NEW, IsolationLevel.SERIALIZABLE,
                reportStart, reportStart.plusMillis(3200),
                reportConnSummary,
                TransactionPhaseStatus.COMMITTED, "report-generator-1",
                Arrays.asList(
                        "SELECT DATE(created_at) as day, COUNT(*) as orders, SUM(total_amount) as revenue FROM orders WHERE created_at >= ? AND created_at < ? GROUP BY DATE(created_at)",
                        "SELECT p.name, SUM(oi.quantity) as sold, SUM(oi.quantity * oi.price) as revenue FROM products p JOIN order_items oi ON p.id = oi.product_id JOIN orders o ON oi.order_id = o.id WHERE o.created_at >= ? GROUP BY p.id ORDER BY sold DESC LIMIT 50",
                        "SELECT customer_segment, COUNT(DISTINCT customer_id) as customers, AVG(total_amount) as avg_order FROM customer_analytics WHERE month = ? GROUP BY customer_segment",
                        "INSERT INTO report_cache (report_type, period, data, generated_at) VALUES ('MONTHLY_SUMMARY', ?, ?, NOW())"
                ),
                Collections.emptyList(),
                reportEvents,
                TX_ALARMING_THRESHOLD
        );

        // Simple Cache Update
        Instant cacheStart = baseTime.plusSeconds(180);
        TransactionLog cacheUpdate = new TransactionLog(
                387, "CacheService.updateUserSession",
                PropagationBehavior.NOT_SUPPORTED, IsolationLevel.DEFAULT,
                cacheStart, cacheStart.plusMillis(15), new ConnectionSummary(0, 0, 0),
                TransactionPhaseStatus.COMMITTED, "session-manager-1",
                Collections.emptyList(),
                Collections.emptyList(),
                Arrays.asList(
                        new TransactionEvent(TransactionEvent.Type.TRANSACTION_START, cacheStart, "Transaction Start [CacheService.updateUserSession]"),
                        new TransactionEvent(TransactionEvent.Type.TRANSACTION_END, cacheStart.plusMillis(15), "Transaction End [CacheService.updateUserSession]")
                ),
                TX_ALARMING_THRESHOLD
        );


        // Analytics Metrics Computation
        Instant analyticsStart = baseTime.plusSeconds(350);
        List<TransactionEvent> analyticsEvents = Arrays.asList(
                new TransactionEvent(TransactionEvent.Type.CONNECTION_ACQUIRED, analyticsStart, "Connection Acquired [1]"),
                new TransactionEvent(TransactionEvent.Type.TRANSACTION_START, analyticsStart, "Transaction Start [AnalyticsService.computeDailyMetrics]"),
                new TransactionEvent(TransactionEvent.Type.TRANSACTION_END, analyticsStart.plusMillis(900), "Transaction End [AnalyticsService.computeDailyMetrics]"),
                new TransactionEvent(TransactionEvent.Type.CONNECTION_RELEASED, analyticsStart.plusMillis(900), "Connection Released [1]")
        );

        ConnectionSummary analyticsConnSummary = fromEvents(analyticsEvents);
        TransactionLog dailyMetrics = new TransactionLog(
                402, "AnalyticsService.computeDailyMetrics",
                PropagationBehavior.REQUIRED, IsolationLevel.READ_COMMITTED,
                analyticsStart, analyticsStart.plusMillis(900),
                analyticsConnSummary,
                TransactionPhaseStatus.COMMITTED, "analytics-worker",
                Arrays.asList(
                        "SELECT COUNT(*) FROM orders WHERE created_at >= ? AND created_at < ?",
                        "INSERT INTO metrics_daily (metric_date, order_count, revenue) VALUES (?, ?, ?)"
                ),
                Collections.emptyList(),
                analyticsEvents,
                TX_ALARMING_THRESHOLD
        );

        Instant emailVerify = baseTime.plusMillis(500);
        TransactionLog emailVerificationWithoutExistingTx = new TransactionLog(
                null, "EmailService.sendVerificationEmail",
                PropagationBehavior.MANDATORY, IsolationLevel.READ_COMMITTED,
                emailVerify, emailVerify.plusMillis(0),
                new ConnectionSummary(0, 0, 0),
                TransactionPhaseStatus.ERRORED, // not executed inside existing transaction
                "auth-service-3",
                Collections.emptyList(),
                Collections.emptyList(),
                Arrays.asList(
                        new TransactionEvent(TransactionEvent.Type.TRANSACTION_START, emailVerify, "Transaction Start [EmailService.sendVerificationEmail]"),
                        new TransactionEvent(TransactionEvent.Type.TRANSACTION_END, emailVerify.plusMillis(0), "Transaction End [EmailService.sendVerificationEmail]")
                ),
                TX_ALARMING_THRESHOLD
        );

        return Arrays.asList(
                orderProcessing,
                userRegistration,
                reportGeneration,
                cacheUpdate,
                dailyMetrics,
                emailVerificationWithoutExistingTx
        );
    }

    public static ConnectionSummary fromEvents(List<TransactionEvent> events) {
        long occupiedTime = 0L;
        int acquisitionCount = 0;
        int alarmingConnectionCount = 0;

        if (events != null && !events.isEmpty()) {
            Deque<TransactionEvent> conEventStack = new LinkedList<>();
            for (TransactionEvent event : events) {
                if (event.getType() == TransactionEvent.Type.CONNECTION_ACQUIRED) {
                    acquisitionCount++;
                    conEventStack.push(event);
                } else if (event.getType() == TransactionEvent.Type.CONNECTION_RELEASED) {
                    TransactionEvent prevEvent = conEventStack.pop();
                    long duration = Duration.between(prevEvent.getTimestamp(), event.getTimestamp()).toMillis();
                    if (duration >= CONNECTION_ALARMING_THRESHOLD) {
                        alarmingConnectionCount++;
                    }
                    occupiedTime += duration;
                }
            }
        }

        return new ConnectionSummary(acquisitionCount, alarmingConnectionCount, occupiedTime);
    }
}
