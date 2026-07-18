package com.rzodeczko.application.port.input

import spock.lang.Specification
import spock.lang.Unroll

class InvoiceIssueResultSpec extends Specification {

    def invoiceId = UUID.randomUUID()

    def "Issued should store invoiceId"() {
        when:
        def result = new InvoiceIssueResult.Issued(invoiceId)

        then:
        result.invoiceId() == invoiceId
        result instanceof InvoiceIssueResult
    }

    def "PendingConfirmation should store invoiceId"() {
        when:
        def result = new InvoiceIssueResult.PendingConfirmation(invoiceId)

        then:
        result.invoiceId() == invoiceId
        result instanceof InvoiceIssueResult
    }

    def "ReconciliationRequired should store invoiceId"() {
        when:
        def result = new InvoiceIssueResult.ReconciliationRequired(invoiceId)

        then:
        result.invoiceId() == invoiceId
        result instanceof InvoiceIssueResult
    }

    def "Issued should implement equals and hashCode"() {
        given:
        def id = UUID.randomUUID()

        expect:
        new InvoiceIssueResult.Issued(id) == new InvoiceIssueResult.Issued(id)
        new InvoiceIssueResult.Issued(id).hashCode() == new InvoiceIssueResult.Issued(id).hashCode()
        new InvoiceIssueResult.Issued(id) != new InvoiceIssueResult.Issued(UUID.randomUUID())
    }

    def "PendingConfirmation should implement equals and hashCode"() {
        given:
        def id = UUID.randomUUID()

        expect:
        new InvoiceIssueResult.PendingConfirmation(id) == new InvoiceIssueResult.PendingConfirmation(id)
        new InvoiceIssueResult.PendingConfirmation(id).hashCode() == new InvoiceIssueResult.PendingConfirmation(id).hashCode()
        new InvoiceIssueResult.PendingConfirmation(id) != new InvoiceIssueResult.PendingConfirmation(UUID.randomUUID())
    }

    def "ReconciliationRequired should implement equals and hashCode"() {
        given:
        def id = UUID.randomUUID()

        expect:
        new InvoiceIssueResult.ReconciliationRequired(id) == new InvoiceIssueResult.ReconciliationRequired(id)
        new InvoiceIssueResult.ReconciliationRequired(id).hashCode() == new InvoiceIssueResult.ReconciliationRequired(id).hashCode()
        new InvoiceIssueResult.ReconciliationRequired(id) != new InvoiceIssueResult.ReconciliationRequired(UUID.randomUUID())
    }

    def "all permitted subtypes should be exactly 3"() {
        expect:
        InvoiceIssueResult.class.permittedSubclasses.length == 3
    }

    def "switch pattern matching should cover all subtypes"() {
        given:
        def results = [
                new InvoiceIssueResult.Issued(invoiceId),
                new InvoiceIssueResult.PendingConfirmation(invoiceId),
                new InvoiceIssueResult.ReconciliationRequired(invoiceId)
        ]

        expect:
        results.every { it instanceof InvoiceIssueResult }
        results.collect { it.invoiceId() }.every { it == invoiceId }
    }
}
