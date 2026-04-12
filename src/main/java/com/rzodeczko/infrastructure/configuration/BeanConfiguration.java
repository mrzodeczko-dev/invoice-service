package com.rzodeczko.infrastructure.configuration;

import com.rzodeczko.application.service.InvoiceService;
import com.rzodeczko.domain.repository.InvoiceRepository;
import com.rzodeczko.infrastructure.configuration.properties.FakturowniaProperties;
import com.rzodeczko.infrastructure.configuration.properties.SwaggerCorsProperties;
import org.jspecify.annotations.NonNull;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.restclient.RestClientCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.net.http.HttpClient;
import java.time.Duration;
import java.util.concurrent.Executors;

@Configuration
@EnableConfigurationProperties({FakturowniaProperties.class, SwaggerCorsProperties.class})
public class BeanConfiguration {


    @Bean
    public WebMvcConfigurer corsConfigurer(SwaggerCorsProperties props) {
        return new WebMvcConfigurer() {
            @Override
            public void addCorsMappings(@NonNull CorsRegistry registry) {
                registry.addMapping("/**")
                        .allowedOrigins(props.getAllowedOrigins())
                        .allowedMethods("GET", "POST", "OPTIONS")
                        .allowedHeaders("*")
                        .maxAge(3600);

            }
        };
    }


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
