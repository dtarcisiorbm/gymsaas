package com.gymsaas.shared.infrastructure.persistence;

import com.zaxxer.hikari.HikariDataSource;
import org.springframework.boot.autoconfigure.jdbc.DataSourceProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

import javax.sql.DataSource;

@Configuration
public class DataSourceConfig {

    @Bean
    @Primary
    public DataSource dataSource(DataSourceProperties properties) {
        HikariDataSource hikari = properties.initializeDataSourceBuilder()
                .type(HikariDataSource.class)
                .build();
        hikari.setPoolName("gymsaas-app");
        hikari.setMaximumPoolSize(10);
        hikari.setMinimumIdle(2);
        hikari.setConnectionTimeout(5_000);
        return new RlsAwareDataSource(hikari);
    }
}
