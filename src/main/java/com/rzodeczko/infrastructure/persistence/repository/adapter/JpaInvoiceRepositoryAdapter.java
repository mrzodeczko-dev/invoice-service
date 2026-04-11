package com.rzodeczko.infrastructure.persistence.repository.adapter;


import com.rzodeczko.application.exception.InvoiceConcurrentModificationException;
import com.rzodeczko.domain.model.Invoice;
import com.rzodeczko.domain.repository.InvoiceRepository;
import com.rzodeczko.infrastructure.persistence.entity.InvoiceEntity;
import com.rzodeczko.infrastructure.persistence.mapper.InvoiceMapper;
import com.rzodeczko.infrastructure.persistence.repository.JpaInvoiceRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.cfg.MapperBuilder;

import java.util.Optional;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class JpaInvoiceRepositoryAdapter implements InvoiceRepository {

    private final JpaInvoiceRepository jpaInvoiceRepository;
    private final InvoiceMapper invoiceMapper;
    private final MapperBuilder mapperBuilder;

    @Override
    @Transactional
    public void save(Invoice invoice) {
        try {
            InvoiceEntity entity = jpaInvoiceRepository
                    .findById(invoice.getId())
                    .map(existing -> {
                        existing.setStatus(invoice.getStatus().name());
                        existing.setExternalId(invoice.getExternalId());
                        return existing;
                    })
                    .orElseGet(() -> invoiceMapper.toEntity(invoice));

            jpaInvoiceRepository.saveAndFlush(entity);
        } catch (ObjectOptimisticLockingFailureException e) {
            throw new InvoiceConcurrentModificationException(invoice.getId());
        }
    }

    @Override
    @Transactional(readOnly = true)
    public boolean existsByOrderId(UUID orderId) {
        return jpaInvoiceRepository.existsByOrderId(orderId);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<Invoice> findById(UUID id) {
        return jpaInvoiceRepository
                .findById(id)
                .map(invoiceMapper::toDomain);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<byte[]> findPdfContent(UUID invoiceId) {
        return jpaInvoiceRepository
                .findPdfContentById(invoiceId)
                .filter(p -> p.length > 0);
    }

    @Override
    @Transactional
    public void savePdfContent(UUID invoiceId, byte[] content) {
        jpaInvoiceRepository.updatePdfContent(invoiceId, content);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<Invoice> findByExternalId(String externalId) {
        return jpaInvoiceRepository
                .findByExternalId(externalId)
                .map(invoiceMapper::toDomain);

    }


}