package com.rzodeczko.application.port.input

import spock.lang.Specification
import spock.lang.Unroll

class GenerateInvoiceCommandSpec extends Specification {

    def validItems = [new ItemCommand("Widget", 1, BigDecimal.TEN, null)]

    def "should create valid command"() {
        when:
        def cmd = new GenerateInvoiceCommand(UUID.randomUUID(), "TAX-1", "Buyer", validItems)

        then:
        noExceptionThrown()
        cmd.items().size() == 1
    }

    @Unroll
    def "should reject when #scenario"() {
        when:
        new GenerateInvoiceCommand(orderId, taxId, buyerName, items)

        then:
        thrown(IllegalArgumentException)

        where:
        scenario                | orderId            | taxId   | buyerName | items
        "orderId is null"       | null               | "TAX-1" | "Buyer"   | [new ItemCommand("W", 1, BigDecimal.TEN, null)]
        "taxId is null"         | UUID.randomUUID()  | null    | "Buyer"   | [new ItemCommand("W", 1, BigDecimal.TEN, null)]
        "taxId is blank"        | UUID.randomUUID()  | "  "    | "Buyer"   | [new ItemCommand("W", 1, BigDecimal.TEN, null)]
        "buyerName is null"     | UUID.randomUUID()  | "TAX-1" | null      | [new ItemCommand("W", 1, BigDecimal.TEN, null)]
        "buyerName is blank"    | UUID.randomUUID()  | "TAX-1" | ""        | [new ItemCommand("W", 1, BigDecimal.TEN, null)]
        "items is null"         | UUID.randomUUID()  | "TAX-1" | "Buyer"   | null
        "items is empty"        | UUID.randomUUID()  | "TAX-1" | "Buyer"   | []
    }
}
