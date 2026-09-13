package com.cursovalido.backend.Forum.configuracao;

import javax.sql.DataSource;

import org.flywaydb.core.Flyway;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.beans.factory.config.BeanFactoryPostProcessor;

@Configuration
public class ConfiguracaoFlyway {

    @Bean(initMethod = "migrate")
    public Flyway flyway(DataSource dataSource) {
        return Flyway.configure()
                .dataSource(dataSource)
                .locations("classpath:db/migration")
                .load();
    }

    @Bean
    public static BeanFactoryPostProcessor executarMigrationAntesDoJpa() {
        return beanFactory -> beanFactory.getBeanDefinition("entityManagerFactory")
                .setDependsOn("flyway");
    }
}