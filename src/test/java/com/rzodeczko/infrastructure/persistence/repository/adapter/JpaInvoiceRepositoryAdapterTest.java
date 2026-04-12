package com.rzodeczko.infrastructure.persistence.repository.adapter;

import com.rzodeczko.application.exception.InvoiceConcurrentModificationException;
import com.rzodeczko.domain.model.Invoice;
import com.rzodeczko.infrastructure.persistence.entity.InvoiceEntity;
import com.rzodeczko.infrastructure.persistence.mapper.InvoiceMapper;
import com.rzodeczko.infrastructure.persistence.repository.JpaInvoiceRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.orm.ObjectOptimisticLockingFailureException;

import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for JpaInvoiceRepositoryAdapter.
 */
class JpaInvoiceRepositoryAdapterTest {
    private JpaInvoiceRepository jpaInvoiceRepository;
    private InvoiceMapper invoiceMapper;
    private JpaInvoiceRepositoryAdapter adapter;

    @BeforeEach
    void setUp() {
        jpaInvoiceRepository = mock(JpaInvoiceRepository.class);
        invoiceMapper = mock(InvoiceMapper.class);
        adapter = new JpaInvoiceRepositoryAdapter(jpaInvoiceRepository, invoiceMapper, null);
    }

    @Test
    void save_shouldSaveAndFlushEntity() {
        Invoice invoice = mock(Invoice.class);
        InvoiceEntity entity = mock(InvoiceEntity.class);
        UUID id = UUID.randomUUID();
        when(invoice.getId()).thenReturn(id);
        when(jpaInvoiceRepository.findById(id)).thenReturn(Optional.empty());
        when(invoiceMapper.toEntity(invoice)).thenReturn(entity);
        adapter.save(invoice);
        verify(jpaInvoiceRepository).saveAndFlush(entity);
    }

    @Test
    void save_shouldThrowOnOptimisticLockingFailure() {
        Invoice invoice = mock(Invoice.class);
        InvoiceEntity entity = mock(InvoiceEntity.class);
        UUID id = UUID.randomUUID();
        when(invoice.getId()).thenReturn(id);
        when(jpaInvoiceRepository.findById(id)).thenThrow(new ObjectOptimisticLockingFailureException("InvoiceEntity", id));
        assertThrows(InvoiceConcurrentModificationException.class, () -> adapter.save(invoice));
    }

    @Test
    void existsByOrderId_shouldDelegate() {
        UUID orderId = UUID.randomUUID();
        when(jpaInvoiceRepository.existsByOrderId(orderId)).thenReturn(true);
        assertTrue(adapter.existsByOrderId(orderId));
    }

    @Test
    void findById_shouldMapToDomain() {
        UUID id = UUID.randomUUID();
        InvoiceEntity entity = mock(InvoiceEntity.class);
        Invoice invoice = mock(Invoice.class);
        when(jpaInvoiceRepository.findById(id)).thenReturn(Optional.of(entity));
        when(invoiceMapper.toDomain(entity)).thenReturn(invoice);
        assertEquals(invoice, adapter.findById(id).orElse(null));
    }

    @Test
    void findPdfContent_shouldReturnFiltered() {
        UUID id = UUID.randomUUID();
        byte[] pdf = new byte[]{1,2,3};
        when(jpaInvoiceRepository.findPdfContentById(id)).thenReturn(Optional.of(pdf));
        assertArrayEquals(pdf, adapter.findPdfContent(id).orElse(null));
    }

    @Test
    void savePdfContent_shouldDelegate() {
        UUID id = UUID.randomUUID();
        byte[] content = new byte[]{1,2,3};
        adapter.savePdfContent(id, content);
        verify(jpaInvoiceRepository).updatePdfContent(id, content);
    }

    @Test
    void findByExternalId_shouldMapToDomain() {
        String externalId = "ext-1";
        InvoiceEntity entity = mock(InvoiceEntity.class);
        Invoice invoice = mock(Invoice.class);
        when(jpaInvoiceRepository.findByExternalId(externalId)).thenReturn(Optional.of(entity));
        when(invoiceMapper.toDomain(entity)).thenReturn(invoice);
        assertEquals(invoice, adapter.findByExternalId(externalId).orElse(null));
    }
}

