package com.rzodeczko.infrastructure.usecase;

import com.rzodeczko.application.exception.EmptyPdfResponseException;
import com.rzodeczko.application.port.input.GenerateInvoiceCommand;
import com.rzodeczko.application.port.input.InvoiceIssueResult;
import com.rzodeczko.application.port.input.ItemCommand;
import com.rzodeczko.application.port.output.TaxSystemPort;
import com.rzodeczko.application.service.InvoiceService;
import com.rzodeczko.domain.exception.InvoiceNotIssuedException;
import com.rzodeczko.domain.exception.ResourceNotFoundException;
import com.rzodeczko.domain.model.Invoice;
import com.rzodeczko.domain.model.InvoiceItem;
import com.rzodeczko.domain.model.InvoiceStatus;
import com.rzodeczko.domain.vo.TaxRate;
import com.rzodeczko.infrastructure.persistence.DataIntegrityViolationClassifier;
import com.rzodeczko.infrastructure.reconciliation.InvoiceReconciliationService;
import com.rzodeczko.infrastructure.transaction.InvoiceTransactionBoundary;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DataIntegrityViolationException;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.*;

@DisplayName("InvoiceUseCaseImpl Unit Tests")
class InvoiceUseCaseImplTest {
    private InvoiceTransactionBoundary invoiceTransactionBoundary;
    private InvoiceService invoiceService;
    private TaxSystemPort taxSystemPort;
    private InvoiceReconciliationService invoiceReconciliationService;
    private DataIntegrityViolationClassifier violationClassifier;
    private InvoiceUseCaseImpl useCase;

    @BeforeEach
    void setUp() {
        invoiceTransactionBoundary = mock(InvoiceTransactionBoundary.class);
        invoiceService = mock(InvoiceService.class);
        taxSystemPort = mock(TaxSystemPort.class);
        invoiceReconciliationService = mock(InvoiceReconciliationService.class);
        violationClassifier = mock(DataIntegrityViolationClassifier.class);
        useCase = new InvoiceUseCaseImpl(
                invoiceTransactionBoundary,
                invoiceService,
                taxSystemPort,
                invoiceReconciliationService,
                violationClassifier
        );
    }

    @Nested
    @DisplayName("generate() tests")
    class GenerateTests {

        @Test
        @DisplayName("Should return Issued result when invoice is successfully issued")
        void shouldReturnIssuedWhenSuccessfullyIssued() {
            // given
            UUID orderId = UUID.randomUUID();
            UUID invoiceId = UUID.randomUUID();

            GenerateInvoiceCommand command = new GenerateInvoiceCommand(
                    orderId,
                    "1234567890",
                    "Real Buyer",
                    List.of(new ItemCommand("Product", 1, BigDecimal.TEN, BigDecimal.valueOf(23)))
            );

            Invoice invoice = Invoice.restore(
                    invoiceId,
                    orderId,
                    "1234567890",
                    "Real Buyer",
                    null,
                    InvoiceStatus.DRAFT,
                    List.of(new InvoiceItem("Product", 1, BigDecimal.TEN, TaxRate.of(23))),
                    Instant.now()
            );

            String externalId = "ext-123";

            given(invoiceService.buildInvoice(command)).willReturn(invoice);
            given(invoiceTransactionBoundary.saveNewInvoice(invoice)).willReturn(invoice);
            given(taxSystemPort.findByOrderId(orderId.toString())).willReturn(List.of());
            given(invoiceReconciliationService.reconcileFromExisting(eq(invoice), anyList())).willReturn(Optional.empty());
            given(taxSystemPort.issueInvoice(invoice)).willReturn(externalId);

            willAnswer(invocation -> {
                Invoice arg = invocation.getArgument(0);
                arg.markAsIssuing();
                return null;
            }).given(invoiceTransactionBoundary).markAsIssuing(any(Invoice.class));

            willAnswer(invocation -> {
                Invoice argInvoice = invocation.getArgument(0);
                String argExternalId = invocation.getArgument(1);
                argInvoice.markAsIssued(argExternalId);
                return null;
            }).given(invoiceTransactionBoundary).markInvoiceAsIssued(any(Invoice.class), anyString());

            // when
            InvoiceIssueResult result = useCase.generate(command);

            // then
            assertThat(result).isInstanceOf(InvoiceIssueResult.Issued.class);
            assertThat(result.invoiceId()).isEqualTo(invoiceId);

            then(invoiceTransactionBoundary).should().markAsIssuing(invoice);
            then(invoiceTransactionBoundary).should().markInvoiceAsIssued(invoice, externalId);

            assertThat(invoice.getStatus()).isEqualTo(InvoiceStatus.ISSUED);
            assertThat(invoice.getExternalId()).isEqualTo(externalId);
        }

        @Test
        @DisplayName("Should recover and return Issued result if invoice already exists in database after DataIntegrityViolationException")
        void shouldRecoverIssuedResultOnConstraintViolation() {
            // given
            UUID orderId = UUID.randomUUID();
            UUID invoiceId = UUID.randomUUID();
            GenerateInvoiceCommand command = new GenerateInvoiceCommand(
                    orderId, "123", "Buyer", List.of(new ItemCommand("Item", 1, BigDecimal.TEN, BigDecimal.valueOf(23)))
            );
            Invoice existingInvoice = Invoice.restore(
                    invoiceId, orderId, "123", "Buyer",
                    "ext-1", InvoiceStatus.ISSUED,
                    List.of(new InvoiceItem("Item", 1, BigDecimal.TEN, TaxRate.of(23))),
                    Instant.now()
            );

            DataIntegrityViolationException exception = new DataIntegrityViolationException("Duplicate");
            given(invoiceService.buildInvoice(command)).willReturn(mock(Invoice.class));
            given(invoiceTransactionBoundary.saveNewInvoice(any())).willThrow(exception);
            given(violationClassifier.isOrderIdUniqueViolation(exception)).willReturn(true);
            given(invoiceTransactionBoundary.findByOrderId(orderId)).willReturn(Optional.of(existingInvoice));

            // when
            InvoiceIssueResult result = useCase.generate(command);

            // then
            assertThat(result).isInstanceOf(InvoiceIssueResult.Issued.class);
            assertThat(result.invoiceId()).isEqualTo(invoiceId);
        }
    }

    @Nested
    @DisplayName("getPdf() tests")
    class GetPdfTests {

        @Test
        @DisplayName("Should return PDF content from cache if available")
        void shouldReturnPdfFromCache() {
            // given
            UUID invoiceId = UUID.randomUUID();
            Invoice invoice = Invoice.restore(
                    invoiceId, UUID.randomUUID(), "123", "Buyer",
                    "ext-1", InvoiceStatus.ISSUED,
                    List.of(new InvoiceItem("Item", 1, BigDecimal.TEN, TaxRate.of(23))),
                    Instant.now()
            );
            byte[] expectedPdf = {1, 2, 3};

            given(invoiceTransactionBoundary.findById(invoiceId)).willReturn(Optional.of(invoice));
            given(invoiceTransactionBoundary.findPdfContent(invoiceId)).willReturn(Optional.of(expectedPdf));

            // when
            byte[] result = useCase.getPdf(invoiceId);

            // then
            assertThat(result).isEqualTo(expectedPdf);
            then(taxSystemPort).shouldHaveNoInteractions();
        }

        @Test
        @DisplayName("Should fetch PDF from TaxSystem and cache it if not in cache")
        void shouldFetchAndCachePdf() {
            // given
            UUID invoiceId = UUID.randomUUID();
            String externalId = "ext-123";
            Invoice invoice = Invoice.restore(
                    invoiceId, UUID.randomUUID(), "123", "Buyer",
                    externalId, InvoiceStatus.ISSUED,
                    List.of(new InvoiceItem("Item", 1, BigDecimal.TEN, TaxRate.of(23))),
                    Instant.now()
            );
            byte[] fetchedPdf = {4, 5, 6};

            given(invoiceTransactionBoundary.findById(invoiceId)).willReturn(Optional.of(invoice));
            given(invoiceTransactionBoundary.findPdfContent(invoiceId)).willReturn(Optional.empty());
            given(taxSystemPort.getPdf(externalId)).willReturn(fetchedPdf);

            // when
            byte[] result = useCase.getPdf(invoiceId);

            // then
            assertThat(result).isEqualTo(fetchedPdf);
            then(invoiceTransactionBoundary).should().savePdfContent(invoiceId, fetchedPdf);
        }

        @Test
        @DisplayName("Should throw ResourceNotFoundException when invoice does not exist")
        void shouldThrowExceptionWhenInvoiceNotFound() {
            // given
            UUID invoiceId = UUID.randomUUID();
            given(invoiceTransactionBoundary.findById(invoiceId)).willReturn(Optional.empty());

            // when / then
            assertThatThrownBy(() -> useCase.getPdf(invoiceId))
                    .isInstanceOf(ResourceNotFoundException.class)
                    .hasMessageContaining(invoiceId.toString());
        }

        @Test
        @DisplayName("Should throw InvoiceNotIssuedException when invoice is not yet issued")
        void shouldThrowExceptionWhenInvoiceNotIssued() {
            // given
            UUID invoiceId = UUID.randomUUID();
            Invoice invoice = Invoice.restore(
                    invoiceId, UUID.randomUUID(), "123", "Buyer",
                    null, InvoiceStatus.DRAFT,
                    List.of(new InvoiceItem("Item", 1, BigDecimal.TEN, TaxRate.of(23))),
                    Instant.now()
            );
            given(invoiceTransactionBoundary.findById(invoiceId)).willReturn(Optional.of(invoice));

            // when / then
            assertThatThrownBy(() -> useCase.getPdf(invoiceId))
                    .isInstanceOf(InvoiceNotIssuedException.class);
        }

        @Test
        @DisplayName("Should throw EmptyPdfResponseException when TaxSystem returns empty PDF")
        void shouldThrowExceptionWhenPdfIsEmpty() {
            // given
            UUID invoiceId = UUID.randomUUID();
            String externalId = "ext-123";
            Invoice invoice = Invoice.restore(
                    invoiceId, UUID.randomUUID(), "123", "Buyer",
                    externalId, InvoiceStatus.ISSUED,
                    List.of(new InvoiceItem("Item", 1, BigDecimal.TEN, TaxRate.of(23))),
                    Instant.now()
            );

            given(invoiceTransactionBoundary.findById(invoiceId)).willReturn(Optional.of(invoice));
            given(invoiceTransactionBoundary.findPdfContent(invoiceId)).willReturn(Optional.empty());
            given(taxSystemPort.getPdf(externalId)).willReturn(new byte[0]);

            // when / then
            assertThatThrownBy(() -> useCase.getPdf(invoiceId))
                    .isInstanceOf(EmptyPdfResponseException.class);
        }
    }
}
