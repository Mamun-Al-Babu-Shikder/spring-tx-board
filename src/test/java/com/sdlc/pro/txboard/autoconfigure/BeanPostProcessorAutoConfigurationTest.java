package com.sdlc.pro.txboard.autoconfigure;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sdlc.pro.txboard.listener.TransactionPhaseListener;
import com.sdlc.pro.txboard.proxy.DataSourceProxy;
import com.sdlc.pro.txboard.proxy.PlatformTransactionManagerProxy;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.transaction.PlatformTransactionManager;

import javax.sql.DataSource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

class BeanPostProcessorAutoConfigurationTest {

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(BeanPostProcessorAutoConfiguration.class, SpringTxBoardAutoConfiguration.class))
            .withBean(ObjectMapper.class, ObjectMapper::new)
            .withBean(DataSource.class, () -> mock(DataSource.class))
            .withBean(PlatformTransactionManager.class, () -> mock(PlatformTransactionManager.class))
            .withPropertyValues("sdlc.pro.spring.tx.board.storage=in_memory");

    @Test
    void shouldExistDataSourceAndPlatformTransactionManagerProxyInstance() {
        contextRunner.run(context -> {
            assertThat(context.getBean(DataSource.class)).isInstanceOf(DataSourceProxy.class);
            assertThat(context.getBean(PlatformTransactionManager.class)).isInstanceOf(PlatformTransactionManagerProxy.class);
        });
    }

    @Test
    void shouldPopulateDataSourceProxyInstanceProperly() {
        contextRunner.run(context -> {
            DataSourceProxy dataSourceProxy = context.getBean(DataSourceProxy.class);
            assertThat(dataSourceProxy).extracting("dataSource").isNotNull();
            TransactionPhaseListener transactionPhaseListener = context.getBean(TransactionPhaseListener.class);
            assertThat(dataSourceProxy).extracting("transactionPhaseListener").isEqualTo(transactionPhaseListener);
        });
    }

    @Test
    void shouldPopulatePlatformTransactionManagerProxyInstanceProperly() {
        contextRunner.run(context -> {
            PlatformTransactionManagerProxy transactionManagerProxy = context.getBean(PlatformTransactionManagerProxy.class);
            assertThat(transactionManagerProxy).extracting("transactionManager").isNotNull();
            TransactionPhaseListener transactionPhaseListener = context.getBean(TransactionPhaseListener.class);
            assertThat(transactionManagerProxy).extracting("transactionPhaseListener").isEqualTo(transactionPhaseListener);
        });
    }
}
