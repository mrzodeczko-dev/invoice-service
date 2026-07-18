package com.rzodeczko.domain.model

import com.rzodeczko.domain.vo.TaxRate
import spock.lang.Specification
import spock.lang.Unroll

class InvoiceItemSpec extends Specification {

    def "should create item with default 23% tax rate"() {
        when:
        def item = new InvoiceItem("Widget", 5, BigDecimal.TEN)

        then:
        item.name() == "Widget"
        item.quantity() == 5
        item.unitPrice() == BigDecimal.TEN
        item.taxRate().value() == new BigDecimal("23")
    }

    def "should create item with explicit tax rate"() {
        when:
        def item = new InvoiceItem("Widget", 1, BigDecimal.ONE, TaxRate.of(8))

        then:
        item.taxRate().intValue() == 8
    }

    @Unroll
    def "should reject quantity=#quantity"() {
        when:
        new InvoiceItem("Widget", quantity, BigDecimal.TEN)

        then:
        thrown(IllegalArgumentException)

        where:
        quantity << [0, -1, -100]
    }

    @Unroll
    def "should reject unitPrice=#unitPrice"() {
        when:
        new InvoiceItem("Widget", 1, unitPrice)

        then:
        thrown(IllegalArgumentException)

        where:
        unitPrice << [null, BigDecimal.ZERO, new BigDecimal("-1")]
    }

    def "should reject null taxRate"() {
        when:
        new InvoiceItem("Widget", 1, BigDecimal.TEN, null)

        then:
        thrown(IllegalArgumentException)
    }
}
