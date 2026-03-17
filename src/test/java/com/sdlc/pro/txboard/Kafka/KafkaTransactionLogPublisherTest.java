package com.sdlc.pro.txboard.Kafka;

import com.sdlc.pro.txboard.enums.TransactionPhaseStatus;
import com.sdlc.pro.txboard.model.TransactionLog;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.kafka.core.KafkaTemplate;

import java.time.Instant;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class KafkaTransactionLogPublisherTest {

    @Mock
    private KafkaTemplate<String, TransactionLog> kafkaTemplate;

    @Mock
    private TxBoardKafkaProperties properties;

    private KafkaTransactionLogPublisher publisher;

    @BeforeEach
    void setUp() {
        publisher = new KafkaTransactionLogPublisher(kafkaTemplate, properties);
    }

    @Nested
    class PublishingTests {

        @Test
        void shouldPublishTransactionLogToKafka() {
            // Given
            String topic = "tx-board-transactions";
            TransactionLog transactionLog = createTestTransactionLog(123);

            when(properties.isEnabled()).thenReturn(true);
            when(properties.getTopic()).thenReturn(topic);
            when(kafkaTemplate.send(eq(topic), anyString(), eq(transactionLog)))
                    .thenReturn(CompletableFuture.completedFuture(null));

            // When
            publisher.listen(transactionLog);

            // Then
            ArgumentCaptor<String> keyCaptor = ArgumentCaptor.forClass(String.class);
            verify(kafkaTemplate, times(1)).send(eq(topic), keyCaptor.capture(), eq(transactionLog));
            assertThat(keyCaptor.getValue()).isEqualTo("123");
        }

        @Test
        void shouldUseTxIdAsKeyWhenAvailable() {
            // Given
            Integer txId = 456;
            TransactionLog transactionLog = createTestTransactionLog(txId);

            when(properties.isEnabled()).thenReturn(true);
            when(properties.getTopic()).thenReturn("test-topic");
            when(kafkaTemplate.send(anyString(), anyString(), any(TransactionLog.class)))
                    .thenReturn(CompletableFuture.completedFuture(null));

            // When
            publisher.listen(transactionLog);

            // Then
            ArgumentCaptor<String> keyCaptor = ArgumentCaptor.forClass(String.class);
            verify(kafkaTemplate).send(anyString(), keyCaptor.capture(), any(TransactionLog.class));
            assertThat(keyCaptor.getValue()).isEqualTo("456");
        }

        @Test
        void shouldGenerateUuidAsKeyWhenTxIdIsNull() {
            // Given
            TransactionLog transactionLog = createTestTransactionLog(null);

            when(properties.isEnabled()).thenReturn(true);
            when(properties.getTopic()).thenReturn("test-topic");
            when(kafkaTemplate.send(anyString(), anyString(), any(TransactionLog.class)))
                    .thenReturn(CompletableFuture.completedFuture(null));

            // When
            publisher.listen(transactionLog);

            // Then
            ArgumentCaptor<String> keyCaptor = ArgumentCaptor.forClass(String.class);
            verify(kafkaTemplate).send(anyString(), keyCaptor.capture(), any(TransactionLog.class));
            String key = keyCaptor.getValue();
            // Verify it's a valid UUID format
            assertThat(key).isNotNull();
            assertThat(UUID.fromString(key)).isNotNull();
        }

        @Test
        void shouldNotPublishWhenDisabled() {
            // Given
            TransactionLog transactionLog = createTestTransactionLog(123);

            when(properties.isEnabled()).thenReturn(false);

            // When
            publisher.listen(transactionLog);

            // Then
            verifyNoInteractions(kafkaTemplate);
        }
    }

    @Nested
    class FailSafeBehaviorTests {

        @Test
        void shouldHandleKafkaSendExceptionGracefully() {
            // Given
            TransactionLog transactionLog = createTestTransactionLog(123);
            RuntimeException kafkaException = new RuntimeException("Kafka connection failed");

            when(properties.isEnabled()).thenReturn(true);
            when(properties.getTopic()).thenReturn("test-topic");
            when(kafkaTemplate.send(anyString(), anyString(), any(TransactionLog.class)))
                    .thenThrow(kafkaException);

            // When/Then - should not throw
            publisher.listen(transactionLog);

            // Verify the send was attempted
            verify(kafkaTemplate, times(1)).send(anyString(), anyString(), any(TransactionLog.class));
        }

        @Test
        void shouldHandleNullPointerExceptionGracefully() {
            // Given
            TransactionLog transactionLog = createTestTransactionLog(123);

            when(properties.isEnabled()).thenReturn(true);
            when(properties.getTopic()).thenReturn(null);
            when(kafkaTemplate.send(any(), anyString(), any(TransactionLog.class)))
                    .thenThrow(new NullPointerException("Topic is null"));

            // When/Then - should not throw
            publisher.listen(transactionLog);

            verify(kafkaTemplate, times(1)).send(any(), anyString(), any(TransactionLog.class));
        }
    }

    @Nested
    class ConfigurationTests {

        @Test
        void shouldUseConfiguredTopic() {
            // Given
            String customTopic = "custom-transaction-topic";
            TransactionLog transactionLog = createTestTransactionLog(123);

            when(properties.isEnabled()).thenReturn(true);
            when(properties.getTopic()).thenReturn(customTopic);
            when(kafkaTemplate.send(anyString(), anyString(), any(TransactionLog.class)))
                    .thenReturn(CompletableFuture.completedFuture(null));

            // When
            publisher.listen(transactionLog);

            // Then
            verify(kafkaTemplate).send(eq(customTopic), anyString(), any(TransactionLog.class));
        }
    }

    private TransactionLog createTestTransactionLog(Integer txId) {
        Instant now = Instant.now();
        return new TransactionLog(
                txId,
                "TestService.testMethod",
                null,
                null,
                now.minusSeconds(1),
                now,
                null,
                TransactionPhaseStatus.COMMITTED,
                Thread.currentThread().getName(),
                java.util.List.of(),
                java.util.List.of(),
                java.util.List.of(),
                1000L,
                java.util.List.of()
        );
    }

}

