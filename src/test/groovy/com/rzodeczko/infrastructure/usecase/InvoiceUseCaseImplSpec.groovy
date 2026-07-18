package com.rzodeczko.infrastructure.usecase

import com.rzodeczko.application.exception.EmptyPdfResponseException
import com.rzodeczko.application.exception.TaxSystemPermanentException
import com.rzodeczko.application.exception.TaxSystemTemporaryException
import com.rzodeczko.application.port.input.GenerateInvoiceCommand
import com.rzodeczko.application.port.input.InvoiceIssueResult
import com.rzodeczko.application.port.input.ItemCommand
import com.rzodeczko.application.port.output.TaxSystemPort
import com.rzodeczko.application.service.InvoiceService
import com.rzodeczko.domain.exception.InvoiceNotIssuedException
import com.rzodeczko.domain.exception.ResourceNotFoundException
import com.rzodeczko.domain.model.Invoice
import com.rzodeczko.domain.model.InvoiceItem
import com.rzodeczko.domain.model.InvoiceStatus
import com.rzodeczko.infrastructure.persistence.DataIntegrityViolationClassifier
import com.rzodeczko.infrastructure.reconciliation.InvoiceReconciliationService
import com.rzodeczko.infrastructure.transaction.InvoiceTransactionBoundary
import org.springframework.dao.DataIntegrityViolationException
import spock.lang.Shared
import spock.lang.Specification
import spock.lang.Subject

class InvoiceUseCaseImplSpec extends Specification {

    InvoiceTransactionBoundary transactionBoundary = Mock()
    InvoiceService invoiceService = Mock()
    TaxSystemPort taxSystemPort = Mock()
    InvoiceReconciliationService reconciliationService = Mock()
    DataIntegrityViolationClassifier violationClassifier = Mock()

    @Subject
    InvoiceUseCaseImpl useCase = new InvoiceUseCaseImpl(
            transactionBoundary, invoiceService, taxSystemPort,
            reconciliationService, violationClassifier
    )

    def invoiceId = UUID.randomUUID()
    def orderId = UUID.randomUUID()
    def validItems = [new InvoiceItem("Item", 1, BigDecimal.TEN)]

    def command = new GenerateInvoiceCommand(
            orderId, "TAX-1", "Buyer",
            [new ItemCommand("Item", 1, BigDecimal.TEN, null)]
    )

    // --- generate: happy path ---

    def "generate should issue new invoice via tax system"() {
        given:
        def draftInvoice = new Invoice(invoiceId, orderId, "TAX-1", "Buyer", validItems)
        invoiceService.buildInvoice(command) >> draftInvoice
        transactionBoundary.saveNewInvoice(draftInvoice) >> draftInvoice
        transactionBoundary.markAsIssuing(draftInvoice) >> { draftInvoice.markAsIssuing() }
        reconciliationService.reconcileFromExisting(draftInvoice, _) >> Optional.empty()
        taxSystemPort.findByOrderId(orderId.toString()) >> []
        taxSystemPort.issueInvoice(draftInvoice) >> "ext-1"

        when:
        def result = useCase.generate(command)

        then:
        1 * transactionBoundary.markInvoiceAsIssued(draftInvoice, "ext-1")
        result instanceof InvoiceIssueResult.Issued
        result.invoiceId() == invoiceId
    }

    def "generate should return Issued when invoice already issued"() {
        given:
        def issuedInvoice = Invoice.restore(invoiceId, orderId, "TAX-1", "Buyer", "ext-1", InvoiceStatus.ISSUED, validItems, null)
        invoiceService.buildInvoice(command) >> issuedInvoice
        transactionBoundary.saveNewInvoice(issuedInvoice) >> issuedInvoice

        when:
        def result = useCase.generate(command)

        then:
        result instanceof InvoiceIssueResult.Issued
    }

    // --- generate: duplicate orderId ---

    def "generate should handle DataIntegrityViolation for duplicate orderId"() {
        given:
        def exception = new DataIntegrityViolationException("uk_invoice_order_id")
        def existingInvoice = Invoice.restore(invoiceId, orderId, "TAX-1", "Buyer", "ext-1", InvoiceStatus.ISSUED, validItems, null)

        invoiceService.buildInvoice(command) >> { throw exception }
        transactionBoundary.saveNewInvoice(_) >> { throw exception }
        violationClassifier.isOrderIdUniqueViolation(exception) >> true
        transactionBoundary.findByOrderId(orderId) >> Optional.of(existingInvoice)

        when:
        def result = useCase.generate(command)

        then:
        result instanceof InvoiceIssueResult.Issued
    }

    def "generate should rethrow DataIntegrityViolation when not orderId violation"() {
        given:
        def exception = new DataIntegrityViolationException("something else")
        def draftInvoice = new Invoice(invoiceId, orderId, "TAX-1", "Buyer", validItems)

        invoiceService.buildInvoice(command) >> draftInvoice
        transactionBoundary.saveNewInvoice(draftInvoice) >> { throw exception }
        violationClassifier.isOrderIdUniqueViolation(exception) >> false

        when:
        useCase.generate(command)

        then:
        thrown(DataIntegrityViolationException)
    }

    // --- generate: temporary failure ---

    def "generate should return PendingConfirmation on temporary tax system failure"() {
        given:
        def draftInvoice = new Invoice(invoiceId, orderId, "TAX-1", "Buyer", validItems)
        invoiceService.buildInvoice(command) >> draftInvoice
        transactionBoundary.saveNewInvoice(draftInvoice) >> draftInvoice
        transactionBoundary.markAsIssuing(draftInvoice) >> { draftInvoice.markAsIssuing() }
        taxSystemPort.findByOrderId(orderId.toString()) >> []
        reconciliationService.reconcileFromExisting(draftInvoice, _) >> Optional.empty()
        taxSystemPort.issueInvoice(draftInvoice) >> { throw new TaxSystemTemporaryException("timeout") }

        when:
        def result = useCase.generate(command)

        then:
        1 * transactionBoundary.markIssueUnknown(draftInvoice)
        result instanceof InvoiceIssueResult.PendingConfirmation
    }

    // --- generate: permanent failure ---

    def "generate should rethrow permanent tax system exception after marking failed"() {
        given:
        def draftInvoice = new Invoice(invoiceId, orderId, "TAX-1", "Buyer", validItems)
        invoiceService.buildInvoice(command) >> draftInvoice
        transactionBoundary.saveNewInvoice(draftInvoice) >> draftInvoice
        transactionBoundary.markAsIssuing(draftInvoice) >> { draftInvoice.markAsIssuing() }
        taxSystemPort.findByOrderId(orderId.toString()) >> []
        reconciliationService.reconcileFromExisting(draftInvoice, _) >> Optional.empty()
        taxSystemPort.issueInvoice(draftInvoice) >> { throw new TaxSystemPermanentException("bad data") }

        when:
        useCase.generate(command)

        then:
        1 * transactionBoundary.markIssueFailed(draftInvoice)
        thrown(TaxSystemPermanentException)
    }

    // --- generate: ISSUE_UNKNOWN status ---

    def "generate should return PendingConfirmation for ISSUE_UNKNOWN invoice"() {
        given:
        def unknownInvoice = Invoice.restore(invoiceId, orderId, "TAX-1", "Buyer", null, InvoiceStatus.ISSUE_UNKNOWN, validItems, null)
        invoiceService.buildInvoice(command) >> unknownInvoice
        transactionBoundary.saveNewInvoice(unknownInvoice) >> unknownInvoice

        when:
        def result = useCase.generate(command)

        then:
        result instanceof InvoiceIssueResult.PendingConfirmation
    }

    // --- generate: RECONCILIATION_REQUIRED status ---

    def "generate should return ReconciliationRequired for that status"() {
        given:
        def reconInvoice = Invoice.restore(invoiceId, orderId, "TAX-1", "Buyer", null, InvoiceStatus.RECONCILIATION_REQUIRED, validItems, null)
        invoiceService.buildInvoice(command) >> reconInvoice
        transactionBoundary.saveNewInvoice(reconInvoice) >> reconInvoice

        when:
        def result = useCase.generate(command)

        then:
        result instanceof InvoiceIssueResult.ReconciliationRequired
    }

    // --- getPdf ---

    def "getPdf should return cached PDF when available"() {
        given:
        def issuedInvoice = Invoice.restore(invoiceId, orderId, "TAX-1", "Buyer", "ext-1", InvoiceStatus.ISSUED, validItems, null)
        def pdfBytes = [1, 2, 3] as byte[]
        transactionBoundary.findById(invoiceId) >> Optional.of(issuedInvoice)
        transactionBoundary.findPdfContent(invoiceId) >> Optional.of(pdfBytes)

        when:
        def result = useCase.getPdf(invoiceId)

        then:
        result == pdfBytes
        0 * taxSystemPort.getPdf(_)
    }

    def "getPdf should fetch from tax system and cache when not in cache"() {
        given:
        def issuedInvoice = Invoice.restore(invoiceId, orderId, "TAX-1", "Buyer", "ext-1", InvoiceStatus.ISSUED, validItems, null)
        def pdfBytes = [4, 5, 6] as byte[]
        transactionBoundary.findById(invoiceId) >> Optional.of(issuedInvoice)
        transactionBoundary.findPdfContent(invoiceId) >> Optional.empty()
        taxSystemPort.getPdf("ext-1") >> pdfBytes

        when:
        def result = useCase.getPdf(invoiceId)

        then:
        result == pdfBytes
        1 * transactionBoundary.savePdfContent(invoiceId, pdfBytes)
    }

    def "getPdf should throw ResourceNotFoundException when invoice not found"() {
        given:
        transactionBoundary.findById(invoiceId) >> Optional.empty()

        when:
        useCase.getPdf(invoiceId)

        then:
        thrown(ResourceNotFoundException)
    }

    def "getPdf should throw InvoiceNotIssuedException when invoice not issued"() {
        given:
        def draftInvoice = new Invoice(invoiceId, orderId, "TAX-1", "Buyer", validItems)
        transactionBoundary.findById(invoiceId) >> Optional.of(draftInvoice)

        when:
        useCase.getPdf(invoiceId)

        then:
        thrown(InvoiceNotIssuedException)
    }

    def "getPdf should throw EmptyPdfResponseException when tax system returns empty"() {
        given:
        def issuedInvoice = Invoice.restore(invoiceId, orderId, "TAX-1", "Buyer", "ext-1", InvoiceStatus.ISSUED, validItems, null)
        transactionBoundary.findById(invoiceId) >> Optional.of(issuedInvoice)
        transactionBoundary.findPdfContent(invoiceId) >> Optional.empty()
        taxSystemPort.getPdf("ext-1") >> new byte[0]

        when:
        useCase.getPdf(invoiceId)

        then:
        thrown(EmptyPdfResponseException)
    }

    def "getPdf should throw EmptyPdfResponseException when tax system returns null"() {
        given:
        def issuedInvoice = Invoice.restore(invoiceId, orderId, "TAX-1", "Buyer", "ext-1", InvoiceStatus.ISSUED, validItems, null)
        transactionBoundary.findById(invoiceId) >> Optional.of(issuedInvoice)
        transactionBoundary.findPdfContent(invoiceId) >> Optional.empty()
        taxSystemPort.getPdf("ext-1") >> null

        when:
        useCase.getPdf(invoiceId)

        then:
        thrown(EmptyPdfResponseException)
    }
}
