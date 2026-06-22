package com.example.pcceobservability.config;

import com.zaxxer.hikari.HikariDataSource;
import javax.sql.DataSource;
import org.springframework.boot.autoconfigure.jdbc.DataSourceProperties;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.JdbcTemplate;

@Configuration
public class DataSourceConfig {

    private final PcceProperties pcceProperties;

    public DataSourceConfig(PcceProperties pcceProperties) {
        this.pcceProperties = pcceProperties;
    }

    @Bean
    @ConfigurationProperties("pcce.datasources.aw")
    DataSourceProperties awDataSourceProperties() {
        return new DataSourceProperties();
    }

    @Bean
    @ConfigurationProperties("pcce.datasources.aw.hikari")
    DataSource awDataSource() {
        return awDataSourceProperties().initializeDataSourceBuilder().type(HikariDataSource.class).build();
    }

    @Bean
    JdbcTemplate awJdbcTemplate() {
        return jdbcTemplate(awDataSource());
    }

    @Bean
    @ConfigurationProperties("pcce.datasources.hds")
    DataSourceProperties hdsDataSourceProperties() {
        return new DataSourceProperties();
    }

    @Bean
    @ConfigurationProperties("pcce.datasources.hds.hikari")
    DataSource hdsDataSource() {
        return hdsDataSourceProperties().initializeDataSourceBuilder().type(HikariDataSource.class).build();
    }

    @Bean
    JdbcTemplate hdsJdbcTemplate() {
        return jdbcTemplate(hdsDataSource());
    }

    @Bean
    @ConfigurationProperties("pcce.datasources.cvp-reporting")
    DataSourceProperties cvpReportingDataSourceProperties() {
        return new DataSourceProperties();
    }

    @Bean
    @ConfigurationProperties("pcce.datasources.cvp-reporting.hikari")
    DataSource cvpReportingDataSource() {
        return cvpReportingDataSourceProperties().initializeDataSourceBuilder().type(HikariDataSource.class).build();
    }

    @Bean
    JdbcTemplate cvpReportingJdbcTemplate() {
        return jdbcTemplate(cvpReportingDataSource());
    }

    private JdbcTemplate jdbcTemplate(DataSource dataSource) {
        JdbcTemplate jdbcTemplate = new JdbcTemplate(dataSource);
        jdbcTemplate.setQueryTimeout(pcceProperties.getPerformance().getJdbcQueryTimeoutSeconds());
        return jdbcTemplate;
    }
}
