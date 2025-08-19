package com.sdlc.pro.txboard.autoconfigure;

import com.sdlc.pro.txboard.proxy.DataSourceProxy;
import com.sdlc.pro.txboard.proxy.PlatformTransactionManagerProxy;
import com.sdlc.pro.txboard.listener.TransactionPhaseListener;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.transaction.PlatformTransactionManager;

import javax.sql.DataSource;

@AutoConfiguration(
        value = "com.sdlc.pro.txboard.autoconfigure.BeanPostProcessorAutoConfiguration",
        before = SpringTxBoardAutoConfiguration.class
)
@ConditionalOnProperty(prefix = "sdlc.pro.spring.tx.board", name = "enable", havingValue = "true", matchIfMissing = true)
public class BeanPostProcessorAutoConfiguration implements BeanPostProcessor, ApplicationContextAware {
    private ApplicationContext applicationContext;

    @Override
    public Object postProcessAfterInitialization(Object bean, String beanName) throws BeansException {
        if (bean instanceof DataSource dataSource) {
            TransactionPhaseListener transactionPhaseListener = this.applicationContext.getBean(TransactionPhaseListener.class);
            return new DataSourceProxy(dataSource, transactionPhaseListener);
        }

        if (bean instanceof PlatformTransactionManager transactionManager) {
            TransactionPhaseListener transactionPhaseListener = this.applicationContext.getBean(TransactionPhaseListener.class);
            return new PlatformTransactionManagerProxy(transactionManager, transactionPhaseListener);
        }

        return BeanPostProcessor.super.postProcessAfterInitialization(bean, beanName);
    }

    @Override
    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        this.applicationContext = applicationContext;
    }
}
