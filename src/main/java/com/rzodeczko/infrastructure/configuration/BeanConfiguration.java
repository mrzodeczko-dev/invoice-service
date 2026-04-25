package com.rzodeczko.infrastructure.configuration;

import com.rzodeczko.application.service.InvoiceService;
import com.rzodeczko.domain.repository.InvoiceRepository;
import com.rzodeczko.infrastructure.configuration.properties.FakturowniaProperties;
import com.rzodeczko.infrastructure.configuration.properties.RedisProperties;
import com.rzodeczko.infrastructure.configuration.properties.WebhookClientsConfig;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.restclient.RestClientCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.JdkClientHttpRequestFactory;

import java.net.http.HttpClient;
import java.time.Duration;
import java.util.concurrent.Executors;

@Configuration
@EnableConfigurationProperties({FakturowniaProperties.class, WebhookClientsConfig.class, RedisProperties.class})
public class BeanConfiguration {


    @Bean
    public RestClientCustomizer restClientCustomizer() {
        HttpClient httpClient = HttpClient
                .newBuilder()
                .connectTimeout(Duration.ofMillis(2000))
                .executor(Executors.newVirtualThreadPerTaskExecutor())
                .build();

        JdkClientHttpRequestFactory requestFactory = new JdkClientHttpRequestFactory(httpClient);
        requestFactory.setReadTimeout(Duration.ofMillis(5000));

        return builder -> builder.requestFactory(requestFactory);
    }

    @Bean
    public InvoiceService invoiceService(InvoiceRepository invoiceRepository) {
        return new InvoiceService(invoiceRepository);
    }

}
