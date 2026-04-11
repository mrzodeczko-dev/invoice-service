package com.rzodeczko.infrastructure.persistence.repository;

import com.rzodeczko.infrastructure.persistence.entity.InvoiceEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;
import java.util.UUID;

public interface JpaInvoiceRepository extends JpaRepository<InvoiceEntity, UUID> {
    boolean existsByOrderId(UUID orderId);

    Optional<InvoiceEntity> findByExternalId(String externalId);

    @Query("select i.pdfContent from InvoiceEntity i where i.id = :id")
    Optional<byte[]> findPdfContentById(@Param("id") UUID id);

    @Modifying
    @Query("update InvoiceEntity i set i.pdfContent = :pdf where i.id = :id")
    void updatePdfContent(@Param("id") UUID id, @Param("pdf") byte[] pdf);
}
