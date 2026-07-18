package com.rzodeczko.presentation.mapper

import com.rzodeczko.application.port.input.InvoiceIssueResult
import org.springframework.http.HttpStatus
import spock.lang.Shared
import spock.lang.Specification
import spock.lang.Subject
import spock.lang.Unroll

class CreateInvoiceResponseMapperSpec extends Specification {

    @Subject
    CreateInvoiceResponseMapper mapper = new CreateInvoiceResponseMapper()

    @Shared
    def invoiceId = UUID.randomUUID()

    @Unroll
    def "should map #resultType to HTTP #expectedStatus with status=#expectedBodyStatus"() {
        when:
        def response = mapper.toResponse(result)

        then:
        response.statusCode == expectedStatus
        response.body.invoiceId() == invoiceId
        response.body.status() == expectedBodyStatus

        where:
        resultType               | result                                                   || expectedStatus      | expectedBodyStatus
        "Issued"                 | new InvoiceIssueResult.Issued(invoiceId)                 || HttpStatus.CREATED  | "ISSUED"
        "PendingConfirmation"    | new InvoiceIssueResult.PendingConfirmation(invoiceId)    || HttpStatus.ACCEPTED | "PENDING_CONFIRMATION"
        "ReconciliationRequired" | new InvoiceIssueResult.ReconciliationRequired(invoiceId) || HttpStatus.CONFLICT | "RECONCILIATION_REQUIRED"
    }

    def "Issued response should contain 'Invoice issued' message"() {
        when:
        def response = mapper.toResponse(new InvoiceIssueResult.Issued(invoiceId))

        then:
        response.body.message() == "Invoice issued"
    }

    def "PendingConfirmation response should contain pending message"() {
        when:
        def response = mapper.toResponse(new InvoiceIssueResult.PendingConfirmation(invoiceId))

        then:
        response.body.message().contains("pending")
    }

    def "ReconciliationRequired response should contain reconciliation message"() {
        when:
        def response = mapper.toResponse(new InvoiceIssueResult.ReconciliationRequired(invoiceId))

        then:
        response.body.message().contains("Reconciliation")
    }
}
