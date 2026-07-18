package com.rzodeczko.infrastructure.fakturownia.adapter

import com.rzodeczko.application.exception.TaxSystemPermanentException
import com.rzodeczko.application.exception.TaxSystemTemporaryException
import com.rzodeczko.domain.model.Invoice
import com.rzodeczko.domain.model.InvoiceItem
import com.rzodeczko.domain.vo.TaxRate
import com.rzodeczko.infrastructure.configuration.properties.FakturowniaProperties
import org.springframework.http.HttpStatus
import org.springframework.web.client.RestClient
import spock.lang.Specification
import spock.lang.Subject
import spock.lang.Unroll

import java.lang.reflect.Method

class FakturowniaAdapterSpec extends Specification {

    @Subject
    FakturowniaAdapter adapter

    def setup() {
        def props = new FakturowniaProperties("https://fakturownia.test", "test-token")
        def builder = RestClient.builder()
        adapter = new FakturowniaAdapter(builder, props)
    }

    // --- handleError ---

    @Unroll
    def "handleError should throw TaxSystemPermanentException for #status"() {
        when:
        adapter.handleError(status, "test", "ctx")

        then:
        thrown(TaxSystemPermanentException)

        where:
        status << [
                HttpStatus.BAD_REQUEST,
                HttpStatus.UNAUTHORIZED,
                HttpStatus.FORBIDDEN,
                HttpStatus.NOT_FOUND,
                HttpStatus.METHOD_NOT_ALLOWED,
                HttpStatus.UNPROCESSABLE_CONTENT,
                HttpStatus.CONFLICT
        ]
    }

    @Unroll
    def "handleError should throw TaxSystemTemporaryException for #status"() {
        when:
        adapter.handleError(status, "test", "ctx")

        then:
        thrown(TaxSystemTemporaryException)

        where:
        status << [
                HttpStatus.REQUEST_TIMEOUT,
                HttpStatus.TOO_MANY_REQUESTS,
                HttpStatus.INTERNAL_SERVER_ERROR,
                HttpStatus.BAD_GATEWAY,
                HttpStatus.SERVICE_UNAVAILABLE,
                HttpStatus.GATEWAY_TIMEOUT
        ]
    }

    def "handleError should throw TaxSystemPermanentException for unhandled 4xx"() {
        when:
        adapter.handleError(HttpStatus.GONE, "test", "ctx")

        then:
        thrown(TaxSystemPermanentException)
    }

    def "handleError should throw TaxSystemTemporaryException for unhandled 5xx"() {
        when:
        adapter.handleError(HttpStatus.NOT_IMPLEMENTED, "test", "ctx")

        then:
        thrown(TaxSystemTemporaryException)
    }

    // --- mapToRequest (still private, uses reflection) ---

    def "mapToRequest should correctly map invoice to request DTO"() {
        given:
        def invoiceId = UUID.randomUUID()
        def orderId = UUID.randomUUID()
        def invoice = new Invoice(invoiceId, orderId, "TAX-123", "Buyer Co",
                [
                        new InvoiceItem("Widget", 3, new BigDecimal("10.00"), TaxRate.of(23)),
                        new InvoiceItem("Gadget", 1, new BigDecimal("50.00"), TaxRate.of(8))
                ])

        Method method = FakturowniaAdapter.getDeclaredMethod("mapToRequest", Invoice)
        method.setAccessible(true)

        when:
        def wrapper = method.invoke(adapter, invoice)

        then:
        wrapper.invoice().kind() == "vat"
        wrapper.invoice().buyerName() == "Buyer Co"
        wrapper.invoice().buyerTaxNo() == "TAX-123"
        wrapper.invoice().orderId() == orderId.toString()
        wrapper.invoice().positions().size() == 2

        and: "first position has quantity * unitPrice as totalPriceGross"
        wrapper.invoice().positions()[0].name() == "Widget"
        wrapper.invoice().positions()[0].tax() == 23
        wrapper.invoice().positions()[0].quantity() == 3
        wrapper.invoice().positions()[0].totalPriceGross() == new BigDecimal("30.00")

        and: "second position"
        wrapper.invoice().positions()[1].name() == "Gadget"
        wrapper.invoice().positions()[1].tax() == 8
        wrapper.invoice().positions()[1].quantity() == 1
        wrapper.invoice().positions()[1].totalPriceGross() == new BigDecimal("50.00")
    }

    def "mapToRequest should set payment_to 7 days after sell_date"() {
        given:
        def invoice = new Invoice(UUID.randomUUID(), UUID.randomUUID(), "TAX-1", "Buyer",
                [new InvoiceItem("Item", 1, BigDecimal.TEN)])

        Method method = FakturowniaAdapter.getDeclaredMethod("mapToRequest", Invoice)
        method.setAccessible(true)

        when:
        def wrapper = method.invoke(adapter, invoice)
        def sellDate = java.time.LocalDate.parse(wrapper.invoice().sellDate())
        def paymentTo = java.time.LocalDate.parse(wrapper.invoice().paymentTo())

        then:
        paymentTo == sellDate.plusDays(7)
        wrapper.invoice().issueDate() == wrapper.invoice().sellDate()
    }
}
