package com.cursovalido.backend.forum.configuration;

import javax.sql.DataSource;
import org.springframework.beans.factory.annotation.Value;

import org.flywaydb.core.Flyway;
import org.flywaydb.core.api.configuration.FluentConfiguration;
import org.springframework.beans.factory.config.BeanFactoryPostProcessor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

@Configuration
@Profile("!test")
public class FlywayConfig {
    @Bean(initMethod = "migrate")
    public Flyway flyway(DataSource dataSource,
            @Value("${spring.flyway.baseline-on-migrate:false}") boolean baselineOnMigrate,
            @Value("${spring.flyway.baseline-version:1}") String baselineVersion) {
        return configuracao()
                .dataSource(dataSource)
                .baselineOnMigrate(baselineOnMigrate)
                .baselineVersion(baselineVersion)
                .validateOnMigrate(true)
                .cleanDisabled(true)
                .load();
    }

    public static FluentConfiguration configuracao() {
        return Flyway.configure().locations("classpath:db/migration");
    }

    @Bean
    public static BeanFactoryPostProcessor flywayBeforeJpa() {
        return beanFactory -> beanFactory.getBeanDefinition("entityManagerFactory")
                .setDependsOn("flyway");
    }

}
