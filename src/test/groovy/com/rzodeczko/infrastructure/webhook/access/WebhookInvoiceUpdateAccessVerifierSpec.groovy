package com.rzodeczko.infrastructure.webhook.access

import com.rzodeczko.infrastructure.configuration.properties.WebhookClientsProperties
import com.rzodeczko.infrastructure.webhook.access.exception.UnauthorizedWebhookAccessException
import spock.lang.Specification
import spock.lang.Subject
import spock.lang.Unroll

class WebhookInvoiceUpdateAccessVerifierSpec extends Specification {

    def "should pass for enabled client with valid secret"() {
        given:
        def config = new WebhookClientsProperties.ClientConfig(true, "secret-123", 60)
        def props = new WebhookClientsProperties(["fakturownia": config])

        @Subject
        def verifier = new WebhookInvoiceUpdateAccessVerifier(props)

        when:
        verifier.verifyEnabledAndSharedSecret("fakturownia", "secret-123")

        then:
        noExceptionThrown()
    }

    def "should throw for unknown client"() {
        given:
        def props = new WebhookClientsProperties([:])

        @Subject
        def verifier = new WebhookInvoiceUpdateAccessVerifier(props)

        when:
        verifier.verifyEnabledAndSharedSecret("unknown-app", "any-token")

        then:
        thrown(UnauthorizedWebhookAccessException)
    }

    def "should throw for disabled client"() {
        given:
        def config = new WebhookClientsProperties.ClientConfig(false, "secret-123", 60)
        def props = new WebhookClientsProperties(["fakturownia": config])

        @Subject
        def verifier = new WebhookInvoiceUpdateAccessVerifier(props)

        when:
        verifier.verifyEnabledAndSharedSecret("fakturownia", "secret-123")

        then:
        thrown(UnauthorizedWebhookAccessException)
    }

    def "should throw for wrong secret"() {
        given:
        def config = new WebhookClientsProperties.ClientConfig(true, "correct-secret", 60)
        def props = new WebhookClientsProperties(["fakturownia": config])

        @Subject
        def verifier = new WebhookInvoiceUpdateAccessVerifier(props)

        when:
        verifier.verifyEnabledAndSharedSecret("fakturownia", "wrong-secret")

        then:
        thrown(UnauthorizedWebhookAccessException)
    }

    @Unroll
    def "should throw when apiToken=#apiToken and sharedSecret=#sharedSecret"() {
        given:
        def config = new WebhookClientsProperties.ClientConfig(true, sharedSecret, 60)
        def props = new WebhookClientsProperties(["app": config])

        @Subject
        def verifier = new WebhookInvoiceUpdateAccessVerifier(props)

        when:
        verifier.verifyEnabledAndSharedSecret("app", apiToken)

        then:
        thrown(UnauthorizedWebhookAccessException)

        where:
        apiToken | sharedSecret
        null     | "secret"
        "token"  | null
        null     | null
    }

    def "should use constant-time comparison (no timing attack)"() {
        given:
        def config = new WebhookClientsProperties.ClientConfig(true, "a" * 1000, 60)
        def props = new WebhookClientsProperties(["app": config])

        @Subject
        def verifier = new WebhookInvoiceUpdateAccessVerifier(props)

        when:
        verifier.verifyEnabledAndSharedSecret("app", "b" * 1000)

        then:
        thrown(UnauthorizedWebhookAccessException)
    }
}
