package com.rzodeczko.infrastructure.configuration;

import com.rzodeczko.application.service.InvoiceService;
import com.rzodeczko.domain.repository.InvoiceRepository;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.mock;

/**
 * Unit tests for BeanConfiguration.
 */
class BeanConfigurationTest {

    private final BeanConfiguration config = new BeanConfiguration();


    @Test
    void restClientCustomizer_shouldCreateCustomizer() {
        assertNotNull(config.restClientCustomizer());
    }

    @Test
    void invoiceService_shouldCreateService() {
        InvoiceRepository repository = mock(InvoiceRepository.class);

        InvoiceService service = config.invoiceService(repository);

        assertNotNull(service);
    }
}
