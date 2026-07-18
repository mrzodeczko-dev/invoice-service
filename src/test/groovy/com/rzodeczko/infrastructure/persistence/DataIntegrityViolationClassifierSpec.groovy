package com.rzodeczko.infrastructure.persistence

import org.springframework.dao.DataIntegrityViolationException
import spock.lang.Specification
import spock.lang.Subject
import spock.lang.Unroll

class DataIntegrityViolationClassifierSpec extends Specification {

    @Subject
    DataIntegrityViolationClassifier classifier = new DataIntegrityViolationClassifier()

    def "should return true when message contains uk_invoice_order_id"() {
        given:
        def ex = new DataIntegrityViolationException("Unique index or primary key violation: uk_invoice_order_id")

        expect:
        classifier.isOrderIdUniqueViolation(ex)
    }

    def "should return true when cause message contains uk_invoice_order_id"() {
        given:
        def cause = new RuntimeException("constraint [uk_invoice_order_id] violated")
        def ex = new DataIntegrityViolationException("could not execute statement", cause)

        expect:
        classifier.isOrderIdUniqueViolation(ex)
    }

    def "should return true when nested cause contains uk_invoice_order_id"() {
        given:
        def root = new RuntimeException("uk_invoice_order_id")
        def mid = new RuntimeException("mid", root)
        def ex = new DataIntegrityViolationException("top", mid)

        expect:
        classifier.isOrderIdUniqueViolation(ex)
    }

    def "should return false when message does not contain the constraint"() {
        given:
        def ex = new DataIntegrityViolationException("some other constraint violated")

        expect:
        !classifier.isOrderIdUniqueViolation(ex)
    }

    def "should return false when message is null at all levels"() {
        given:
        def cause = new RuntimeException((String) null)
        def ex = new DataIntegrityViolationException(null, cause)

        expect:
        !classifier.isOrderIdUniqueViolation(ex)
    }
}
