package com.rzodeczko

import com.rzodeczko.infrastructure.configuration.JpaAuditingConfig
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest
import org.springframework.context.annotation.Import
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.context.DynamicPropertyRegistry
import org.springframework.test.context.DynamicPropertySource
import org.testcontainers.mysql.MySQLContainer
import org.testcontainers.spock.Testcontainers
import spock.lang.Shared
import spock.lang.Specification

@Testcontainers
@DataJpaTest
@Import(JpaAuditingConfig)
@ActiveProfiles("testcontainers")
abstract class JpaIntegrationSpec extends Specification {

    @Shared
    static MySQLContainer mysql = new MySQLContainer("mysql:8.0")
            .withDatabaseName("invoice_service_test")
            .withUsername("test")
            .withPassword("test")

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", mysql::getJdbcUrl)
        registry.add("spring.datasource.username", mysql::getUsername)
        registry.add("spring.datasource.password", mysql::getPassword)
    }
}
