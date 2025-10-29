package com.sdlc.pro.txboard.autoconfigure;

import com.sdlc.pro.txboard.config.SpringTxBoardWebConfiguration;
import com.sdlc.pro.txboard.config.TxBoardProperties;
import com.sdlc.pro.txboard.listener.TransactionLogListener;
import com.sdlc.pro.txboard.listener.TransactionPhaseListener;
import com.sdlc.pro.txboard.listener.TransactionPhaseListenerImpl;
import org.springframework.boot.autoconfigure.AutoConfigureAfter;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Configuration;
import org.springframework.transaction.PlatformTransactionManager;

import java.util.List;

@Configuration
@AutoConfigureAfter(BeanPostProcessorAutoConfiguration.class)
@ConditionalOnClass(PlatformTransactionManager.class)
@EnableConfigurationProperties(TxBoardProperties.class)
@Import({SpringTxBoardWebConfiguration.class})
@ConditionalOnProperty(prefix = "sdlc.pro.spring.tx.board", name = "enabled", havingValue = "true", matchIfMissing = true)
public class SpringTxBoardAutoConfiguration {

    @Bean("sdlcProTxPhaseListener")
    public TransactionPhaseListener transactionPhaseListener(List<TransactionLogListener> transactionLogListeners, TxBoardProperties txBoardProperties) {
        return new TransactionPhaseListenerImpl(transactionLogListeners, txBoardProperties);
    }
}
