package com.rzodeczko

import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.context.DynamicPropertyRegistry
import org.springframework.test.context.DynamicPropertySource
import org.testcontainers.containers.GenericContainer
import org.testcontainers.mysql.MySQLContainer
import org.testcontainers.spock.Testcontainers
import spock.lang.Shared
import spock.lang.Specification

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("testcontainers")
@Testcontainers(disabledWithoutDocker = true)
abstract class IntegrationSpec extends Specification {

    @Shared
    static MySQLContainer mysql = new MySQLContainer("mysql:9.6.0")
            .withDatabaseName("invoice_service_test")
            .withUsername("test")
            .withPassword("test")

    @Shared
    static GenericContainer redis = new GenericContainer("redis:7-alpine")
            .withExposedPorts(6379)


    static {
        mysql.start()
        redis.start()
    }




    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", mysql::getJdbcUrl)
        registry.add("spring.datasource.username", mysql::getUsername)
        registry.add("spring.datasource.password", mysql::getPassword)
        registry.add("spring.data.redis.host", redis::getHost)
        registry.add("spring.data.redis.port", { redis.getMappedPort(6379) })
    }
}
