package com.rzodeczko.infrastructure.webhook.access.aop

import com.rzodeczko.infrastructure.webhook.access.TrustedWebhookClient
import com.rzodeczko.infrastructure.webhook.access.WebhookInvoiceUpdateAccessVerifier
import com.rzodeczko.infrastructure.webhook.access.exception.UnauthorizedWebhookAccessException
import com.rzodeczko.infrastructure.webhook.access.ratelimiter.ClientRateLimiter
import com.rzodeczko.presentation.dto.FakturowniaWebhookDealDto
import com.rzodeczko.presentation.dto.FakturowniaWebhookDto
import spock.lang.Specification
import spock.lang.Subject

class WebhookSecurityAspectSpec extends Specification {

    WebhookInvoiceUpdateAccessVerifier accessVerifier = Mock()
    ClientRateLimiter rateLimiter = Mock()

    @Subject
    WebhookSecurityAspect aspect = new WebhookSecurityAspect(accessVerifier, rateLimiter)

    def webhookSecured = Stub(WebhookSecured) {
        value() >> ([TrustedWebhookClient.FAKTUROWNIA] as TrustedWebhookClient[])
    }

    def "should pass verification for trusted client with valid credentials"() {
        given:
        def payload = new FakturowniaWebhookDto(1L, new FakturowniaWebhookDealDto([:]), "fakturownia", "token-123")

        when:
        aspect.verifyAccessAndRateLimit(webhookSecured, payload)

        then:
        1 * accessVerifier.verifyEnabledAndSharedSecret("fakturownia", "token-123")
        1 * rateLimiter.check("fakturownia")
    }

    def "should throw for untrusted client"() {
        given:
        def payload = new FakturowniaWebhookDto(1L, null, "unknown-app", "token")

        when:
        aspect.verifyAccessAndRateLimit(webhookSecured, payload)

        then:
        thrown(UnauthorizedWebhookAccessException)
        0 * accessVerifier._
        0 * rateLimiter._
    }

    def "should throw for null appName"() {
        given:
        def payload = new FakturowniaWebhookDto(1L, null, null, "token")

        when:
        aspect.verifyAccessAndRateLimit(webhookSecured, payload)

        then:
        thrown(UnauthorizedWebhookAccessException)
    }

    def "should propagate exception from accessVerifier"() {
        given:
        def payload = new FakturowniaWebhookDto(1L, null, "fakturownia", "bad-token")
        accessVerifier.verifyEnabledAndSharedSecret("fakturownia", "bad-token") >> {
            throw new UnauthorizedWebhookAccessException("Invalid webhook sharedSecret")
        }

        when:
        aspect.verifyAccessAndRateLimit(webhookSecured, payload)

        then:
        thrown(UnauthorizedWebhookAccessException)
        0 * rateLimiter._
    }

    def "should propagate exception from rateLimiter"() {
        given:
        def payload = new FakturowniaWebhookDto(1L, null, "fakturownia", "token")
        rateLimiter.check("fakturownia") >> { throw new RuntimeException("rate limit") }

        when:
        aspect.verifyAccessAndRateLimit(webhookSecured, payload)

        then:
        thrown(RuntimeException)
    }
}
