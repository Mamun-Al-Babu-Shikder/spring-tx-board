package integration.config;

import javax.persistence.EntityManagerFactory;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.jdbc.support.JdbcTransactionManager;
import org.springframework.orm.jpa.JpaTransactionManager;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.EnableTransactionManagement;

import javax.sql.DataSource;

@Configuration
@EnableAutoConfiguration
@EnableTransactionManagement
@ComponentScan(basePackages = "integration")
public class TestTxBoardConfig {

    @Bean
    @Primary
    public PlatformTransactionManager transactionManager(EntityManagerFactory emf) {
        return new JpaTransactionManager(emf);
    }

    @Bean
    public JdbcTransactionManager jdbcTransactionManager(DataSource dataSource) {
        return new JdbcTransactionManager(dataSource);
    }
}
