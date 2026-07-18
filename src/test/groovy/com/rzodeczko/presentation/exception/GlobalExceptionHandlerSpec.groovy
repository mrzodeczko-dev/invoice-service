package com.rzodeczko.presentation.exception

import com.rzodeczko.application.exception.EmptyPdfResponseException
import com.rzodeczko.application.exception.InvoiceConcurrentModificationException
import com.rzodeczko.application.exception.TaxSystemPermanentException
import com.rzodeczko.application.exception.TaxSystemTemporaryException
import com.rzodeczko.domain.exception.InvoiceAlreadyExistsException
import com.rzodeczko.domain.exception.InvoiceNotIssuedException
import com.rzodeczko.domain.exception.ResourceNotFoundException
import com.rzodeczko.infrastructure.webhook.access.exception.UnauthorizedWebhookAccessException
import com.rzodeczko.infrastructure.webhook.access.exception.WebhookRateLimitExceededException
import org.springframework.dao.DataIntegrityViolationException
import org.springframework.http.HttpMethod
import org.springframework.http.HttpStatus
import org.springframework.web.servlet.resource.NoResourceFoundException
import spock.lang.Specification
import spock.lang.Subject
import spock.lang.Unroll

class GlobalExceptionHandlerSpec extends Specification {

    @Subject
    GlobalExceptionHandler handler = new GlobalExceptionHandler()

    @Unroll
    def "should map #exceptionName to HTTP #expectedStatus"() {
        when:
        def response = handler.handle(exception)

        then:
        response.statusCode == expectedStatus
        response.body.status() == expectedStatus.value()

        where:
        exceptionName                       | exception                                                     || expectedStatus
        "UnauthorizedWebhookAccess"         | new UnauthorizedWebhookAccessException("bad token")           || HttpStatus.UNAUTHORIZED
        "WebhookRateLimitExceeded"          | new WebhookRateLimitExceededException("too many")             || HttpStatus.TOO_MANY_REQUESTS
        "IllegalArgument"                   | new IllegalArgumentException("bad arg")                       || HttpStatus.BAD_REQUEST
        "ResourceNotFound"                  | new ResourceNotFoundException("not found")                    || HttpStatus.NOT_FOUND
        "InvoiceAlreadyExists"              | new InvoiceAlreadyExistsException(UUID.randomUUID())          || HttpStatus.CONFLICT
        "InvoiceNotIssued"                  | new InvoiceNotIssuedException(UUID.randomUUID())              || HttpStatus.CONFLICT
        "InvoiceConcurrentModification"     | new InvoiceConcurrentModificationException(UUID.randomUUID()) || HttpStatus.CONFLICT
        "DataIntegrityViolation"            | new DataIntegrityViolationException("dup")                    || HttpStatus.CONFLICT
        "TaxSystemPermanent"                | new TaxSystemPermanentException("perm error")                 || HttpStatus.UNPROCESSABLE_CONTENT
        "TaxSystemTemporary"                | new TaxSystemTemporaryException("temp error")                 || HttpStatus.SERVICE_UNAVAILABLE
        "EmptyPdfResponse"                  | new EmptyPdfResponseException("empty")                        || HttpStatus.BAD_GATEWAY
        "IllegalState"                      | new IllegalStateException("bad state")                        || HttpStatus.INTERNAL_SERVER_ERROR
    }

    def "should map generic Exception to 500 with generic message"() {
        when:
        def response = handler.handle(new RuntimeException("unexpected"))

        then:
        response.statusCode == HttpStatus.INTERNAL_SERVER_ERROR
        response.body.status() == 500
        response.body.message() == "Unexpected error"
    }

    def "should map NoResourceFoundException to 404"() {
        given:
        def ex = new NoResourceFoundException(HttpMethod.GET, "/unknown", "/unknown")

        when:
        def response = handler.handle(ex)

        then:
        response.statusCode == HttpStatus.NOT_FOUND
        response.body.status() == 404
    }

    def "TaxSystemTemporary response should include original message"() {
        when:
        def response = handler.handle(new TaxSystemTemporaryException("connection timeout"))

        then:
        response.body.message().contains("connection timeout")
    }

    def "DataIntegrityViolation response should not leak internal details"() {
        when:
        def response = handler.handle(new DataIntegrityViolationException("SQL [insert ...]; constraint [uk_xyz]"))

        then:
        !response.body.message().contains("SQL")
        !response.body.message().contains("constraint")
        response.body.message().contains("data integrity")
    }

    def "InvoiceConcurrentModification response should suggest retry"() {
        when:
        def response = handler.handle(new InvoiceConcurrentModificationException(UUID.randomUUID()))

        then:
        response.body.message().contains("retry")
    }
}
