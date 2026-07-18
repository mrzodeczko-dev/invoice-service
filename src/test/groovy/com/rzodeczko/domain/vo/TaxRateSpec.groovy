package com.rzodeczko.domain.vo

import spock.lang.Specification
import spock.lang.Unroll

class TaxRateSpec extends Specification {

    def "should create tax rate from BigDecimal"() {
        when:
        def rate = TaxRate.of(new BigDecimal("23"))

        then:
        rate.value() == new BigDecimal("23")
        rate.intValue() == 23
    }

    def "should create tax rate from double"() {
        when:
        def rate = TaxRate.of(8.5)

        then:
        rate.value() == new BigDecimal("8.5")
        rate.intValue() == 8
    }

    def "should allow zero tax rate"() {
        when:
        def rate = TaxRate.of(0)

        then:
        rate.value() == new BigDecimal("0.0")
    }

    @Unroll
    def "should reject negative value #value"() {
        when:
        new TaxRate(value)

        then:
        thrown(IllegalArgumentException)

        where:
        value << [new BigDecimal("-1"), new BigDecimal("-0.01")]
    }

    def "should reject null value"() {
        when:
        new TaxRate(null)

        then:
        thrown(IllegalArgumentException)
    }
}
