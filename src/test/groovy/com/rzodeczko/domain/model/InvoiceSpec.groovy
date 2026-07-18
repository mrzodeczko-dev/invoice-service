package com.rzodeczko.domain.model

import spock.lang.Shared
import spock.lang.Specification
import spock.lang.Unroll

import java.time.Instant

class InvoiceSpec extends Specification {

    static final UUID ID = UUID.randomUUID()
    static final UUID ORDER_ID = UUID.randomUUID()
    static final String TAX_ID = "123-456-78-90"
    static final String BUYER = "Test Buyer"

    @Shared
    def validItems = [new InvoiceItem("Item", 1, BigDecimal.TEN)]

    // --- creation ---

    def "new invoice should have DRAFT status and no externalId"() {
        when:
        def invoice = new Invoice(ID, ORDER_ID, TAX_ID, BUYER, validItems)

        then:
        invoice.status == InvoiceStatus.DRAFT
        invoice.externalId == null
        invoice.isDraft()
        !invoice.isIssued()
        !invoice.isIssuing()
    }

    def "items list should be immutable"() {
        given:
        def mutableItems = new ArrayList<>(validItems)
        def invoice = new Invoice(ID, ORDER_ID, TAX_ID, BUYER, mutableItems)

        when:
        invoice.items.add(new InvoiceItem("Extra", 1, BigDecimal.ONE))

        then:
        thrown(UnsupportedOperationException)
    }

    def "null items list should default to empty and fail validation"() {
        when:
        new Invoice(ID, ORDER_ID, TAX_ID, BUYER, null)

        then:
        thrown(IllegalStateException)
    }

    @Unroll
    def "creation should fail when #scenario"() {
        when:
        new Invoice(id, orderId, taxId, buyerName, items)

        then:
        thrown(expectedException)

        where:
        scenario              | id   | orderId  | taxId  | buyerName | items      || expectedException
        "id is null"          | null | ORDER_ID | TAX_ID | BUYER     | validItems || IllegalArgumentException
        "orderId is null"     | ID   | null     | TAX_ID | BUYER     | validItems || IllegalArgumentException
        "taxId is null"       | ID   | ORDER_ID | null   | BUYER     | validItems || IllegalArgumentException
        "taxId is blank"      | ID   | ORDER_ID | "  "   | BUYER     | validItems || IllegalArgumentException
        "buyerName is null"   | ID   | ORDER_ID | TAX_ID | null      | validItems || IllegalArgumentException
        "buyerName is blank"  | ID   | ORDER_ID | TAX_ID | ""        | validItems || IllegalArgumentException
        "items list is empty" | ID   | ORDER_ID | TAX_ID | BUYER     | []         || IllegalStateException
    }

    // --- restore ---

    def "restore should create invoice with all fields including createdAt"() {
        given:
        def now = Instant.now()

        when:
        def invoice = Invoice.restore(ID, ORDER_ID, TAX_ID, BUYER, "ext-1", InvoiceStatus.ISSUED, validItems, now)

        then:
        invoice.id == ID
        invoice.orderId == ORDER_ID
        invoice.externalId == "ext-1"
        invoice.status == InvoiceStatus.ISSUED
        invoice.createdAt == now
    }

    def "restore with ISSUED status but no externalId should fail"() {
        when:
        Invoice.restore(ID, ORDER_ID, TAX_ID, BUYER, null, InvoiceStatus.ISSUED, validItems, null)

        then:
        thrown(IllegalStateException)
    }

    def "restore with non-ISSUED status but with externalId should fail"() {
        when:
        Invoice.restore(ID, ORDER_ID, TAX_ID, BUYER, "ext-1", InvoiceStatus.DRAFT, validItems, null)

        then:
        thrown(IllegalStateException)
    }

    // --- state transitions ---

    def "markAsIssuing should transition DRAFT to ISSUING"() {
        given:
        def invoice = new Invoice(ID, ORDER_ID, TAX_ID, BUYER, validItems)

        when:
        invoice.markAsIssuing()

        then:
        invoice.isIssuing()
        invoice.status == InvoiceStatus.ISSUING
    }

    def "markAsIssuing from ISSUED should throw"() {
        given:
        def invoice = new Invoice(ID, ORDER_ID, TAX_ID, BUYER, validItems)
        invoice.markAsIssued("ext-1")

        when:
        invoice.markAsIssuing()

        then:
        thrown(IllegalStateException)
    }

    @Unroll
    def "markAsIssued should work from #fromStatus"() {
        given:
        def invoice = new Invoice(ID, ORDER_ID, TAX_ID, BUYER, validItems)
        if (fromStatus == InvoiceStatus.ISSUING) {
            invoice.markAsIssuing()
        }

        when:
        invoice.markAsIssued("ext-1")

        then:
        invoice.isIssued()
        invoice.externalId == "ext-1"

        where:
        fromStatus << [InvoiceStatus.DRAFT, InvoiceStatus.ISSUING]
    }

    def "markAsIssued with blank externalId should throw"() {
        given:
        def invoice = new Invoice(ID, ORDER_ID, TAX_ID, BUYER, validItems)

        when:
        invoice.markAsIssued("  ")

        then:
        thrown(IllegalArgumentException)
    }

    def "markAsIssued with null externalId should throw"() {
        given:
        def invoice = new Invoice(ID, ORDER_ID, TAX_ID, BUYER, validItems)

        when:
        invoice.markAsIssued(null)

        then:
        thrown(IllegalArgumentException)
    }

    def "markAsIssueFailed should set ISSUE_FAILED status"() {
        given:
        def invoice = new Invoice(ID, ORDER_ID, TAX_ID, BUYER, validItems)

        when:
        invoice.markAsIssueFailed()

        then:
        invoice.status == InvoiceStatus.ISSUE_FAILED
    }

    def "markAsIssueUnknown should set ISSUE_UNKNOWN status"() {
        given:
        def invoice = new Invoice(ID, ORDER_ID, TAX_ID, BUYER, validItems)

        when:
        invoice.markAsIssueUnknown()

        then:
        invoice.isIssueUnknown()
        invoice.status == InvoiceStatus.ISSUE_UNKNOWN
    }

    def "markAsReconciliationRequired should set status and clear externalId"() {
        given:
        def invoice = new Invoice(ID, ORDER_ID, TAX_ID, BUYER, validItems)

        when:
        invoice.markAsReconciliationRequired()

        then:
        invoice.isReconciliationRequired()
        invoice.status == InvoiceStatus.RECONCILIATION_REQUIRED
        invoice.externalId == null
    }

    // --- boolean status helpers ---

    @Unroll
    def "#method should return #expected for status #status"() {
        given:
        def invoice = Invoice.restore(ID, ORDER_ID, TAX_ID, BUYER,
                status == InvoiceStatus.ISSUED ? "ext-1" : null,
                status, validItems, null)

        expect:
        invoice."$method"() == expected

        where:
        status                                | method                     || expected
        InvoiceStatus.DRAFT                   | "isDraft"                  || true
        InvoiceStatus.DRAFT                   | "isIssued"                 || false
        InvoiceStatus.ISSUING                 | "isIssuing"                || true
        InvoiceStatus.ISSUING                 | "isDraft"                  || false
        InvoiceStatus.ISSUED                  | "isIssued"                 || true
        InvoiceStatus.ISSUE_UNKNOWN           | "isIssueUnknown"           || true
        InvoiceStatus.RECONCILIATION_REQUIRED | "isReconciliationRequired" || true
    }
}
