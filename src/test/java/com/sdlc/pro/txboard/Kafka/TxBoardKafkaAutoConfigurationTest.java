package com.sdlc.pro.txboard.Kafka;

import com.sdlc.pro.txboard.listener.TransactionLogListener;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.FilteredClassLoader;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.kafka.core.KafkaTemplate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

class TxBoardKafkaAutoConfigurationTest {

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(TxBoardKafkaAutoConfiguration.class));

    @Test
    void shouldNotAutoConfigureWhenKafkaTemplateClassMissing() {
        contextRunner
                .withClassLoader(new FilteredClassLoader("org.springframework.kafka"))
                .run(context -> assertThat(context).doesNotHaveBean(TxBoardKafkaAutoConfiguration.class));
    }

    @Test
    void shouldNotCreatePublisherWhenKafkaTemplateBeanMissing() {
        contextRunner
                .run(context -> assertThat(context).doesNotHaveBean("sdlcProKafkaTransactionLogPublisher"));
    }

    @Nested
    class WithKafkaTemplate {

        @Test
        void shouldCreateKafkaPublisherWhenKafkaTemplateAvailable() {
            contextRunner
                    .withBean(KafkaTemplate.class, () -> mock(KafkaTemplate.class))
                    .run(context -> {
                        assertThat(context).hasBean("sdlcProKafkaTransactionLogPublisher");
                        assertThat(context.getBean("sdlcProKafkaTransactionLogPublisher"))
                                .isInstanceOf(TransactionLogListener.class)
                                .isInstanceOf(KafkaTransactionLogPublisher.class);
                    });
        }

        @Test
        void shouldCreateTxBoardKafkaProperties() {
            contextRunner
                    .withBean(KafkaTemplate.class, () -> mock(KafkaTemplate.class))
                    .run(context -> assertThat(context).hasSingleBean(TxBoardKafkaProperties.class));
        }

        @Test
        void shouldUseDefaultTopicName() {
            contextRunner
                    .withBean(KafkaTemplate.class, () -> mock(KafkaTemplate.class))
                    .run(context -> {
                        TxBoardKafkaProperties properties = context.getBean(TxBoardKafkaProperties.class);
                        assertThat(properties.getTopic()).isEqualTo("tx-board-transactions");
                        assertThat(properties.isEnabled()).isTrue();
                    });
        }

        @Test
        void shouldUseCustomTopicNameFromProperties() {
            contextRunner
                    .withBean(KafkaTemplate.class, () -> mock(KafkaTemplate.class))
                    .withPropertyValues("sdlc.pro.spring.tx.board.kafka.topic=custom-topic")
                    .run(context -> {
                        TxBoardKafkaProperties properties = context.getBean(TxBoardKafkaProperties.class);
                        assertThat(properties.getTopic()).isEqualTo("custom-topic");
                    });
        }

        @Test
        void shouldRespectEnabledProperty() {
            contextRunner
                    .withBean(KafkaTemplate.class, () -> mock(KafkaTemplate.class))
                    .withPropertyValues("sdlc.pro.spring.tx.board.kafka.enabled=false")
                    .run(context -> {
                        TxBoardKafkaProperties properties = context.getBean(TxBoardKafkaProperties.class);
                        assertThat(properties.isEnabled()).isFalse();
                    });
        }

        @Test
        void shouldEnableByDefault() {
            contextRunner
                    .withBean(KafkaTemplate.class, () -> mock(KafkaTemplate.class))
                    .run(context -> {
                        TxBoardKafkaProperties properties = context.getBean(TxBoardKafkaProperties.class);
                        assertThat(properties.isEnabled()).isTrue();
                    });
        }
    }
}

