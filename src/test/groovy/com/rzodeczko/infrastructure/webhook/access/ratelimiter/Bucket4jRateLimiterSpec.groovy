package com.rzodeczko.infrastructure.webhook.access.ratelimiter

import com.rzodeczko.infrastructure.configuration.properties.WebhookClientsProperties
import com.rzodeczko.infrastructure.webhook.access.exception.WebhookRateLimitExceededException
import io.github.bucket4j.distributed.BucketProxy
import io.github.bucket4j.ConsumptionProbe
import io.github.bucket4j.distributed.proxy.ProxyManager
import io.github.bucket4j.distributed.proxy.RemoteBucketBuilder
import spock.lang.Specification
import spock.lang.Subject

import java.util.function.Supplier

class Bucket4jRateLimiterSpec extends Specification {

    ProxyManager<String> proxyManager = Mock()
    WebhookClientsProperties webhookClientsProperties = Mock()

    @Subject
    Bucket4jRateLimiter rateLimiter = new Bucket4jRateLimiter(proxyManager, webhookClientsProperties)

    def "should allow request when token is consumed"() {
        given:
        def clientConfig = new WebhookClientsProperties.ClientConfig(true, "secret", 60)
        webhookClientsProperties.clients() >> Map.of("client-1", clientConfig)

        def probe = Mock(ConsumptionProbe)
        probe.isConsumed() >> true

        def bucket = Mock(BucketProxy)
        bucket.tryConsumeAndReturnRemaining(1) >> probe

        def builder = Mock(RemoteBucketBuilder)
        builder.build(_ as String, _ as Supplier) >> bucket
        proxyManager.builder() >> builder

        when:
        rateLimiter.check("client-1")

        then:
        noExceptionThrown()
    }

    def "should throw WebhookRateLimitExceededException when token is not consumed"() {
        given:
        def clientConfig = new WebhookClientsProperties.ClientConfig(true, "secret", 10)
        webhookClientsProperties.clients() >> Map.of("client-1", clientConfig)

        def probe = Mock(ConsumptionProbe)
        probe.isConsumed() >> false
        probe.getNanosToWaitForRefill() >> 30_000_000_000L // 30 seconds

        def bucket = Mock(BucketProxy)
        bucket.tryConsumeAndReturnRemaining(1) >> probe

        def builder = Mock(RemoteBucketBuilder)
        builder.build(_ as String, _ as Supplier) >> bucket
        proxyManager.builder() >> builder

        when:
        rateLimiter.check("client-1")

        then:
        def ex = thrown(WebhookRateLimitExceededException)
        ex.message.contains("client-1")
        ex.message.contains("30 seconds")
    }

    def "should skip rate limiting when client has no config"() {
        given:
        webhookClientsProperties.clients() >> Map.of()

        when:
        rateLimiter.check("unknown-client")

        then:
        noExceptionThrown()
        0 * proxyManager._
    }

    def "should skip rate limiting when requestsPerMinuteLimit is 0"() {
        given:
        def clientConfig = new WebhookClientsProperties.ClientConfig(true, "secret", 0)
        webhookClientsProperties.clients() >> Map.of("client-1", clientConfig)

        when:
        rateLimiter.check("client-1")

        then:
        noExceptionThrown()
        0 * proxyManager._
    }

    def "should skip rate limiting when requestsPerMinuteLimit is negative"() {
        given:
        def clientConfig = new WebhookClientsProperties.ClientConfig(true, "secret", -1)
        webhookClientsProperties.clients() >> Map.of("client-1", clientConfig)

        when:
        rateLimiter.check("client-1")

        then:
        noExceptionThrown()
        0 * proxyManager._
    }

    def "should use correct Redis key with client prefix"() {
        given:
        def clientConfig = new WebhookClientsProperties.ClientConfig(true, "secret", 100)
        webhookClientsProperties.clients() >> Map.of("my-client", clientConfig)

        def probe = Mock(ConsumptionProbe)
        probe.isConsumed() >> true

        def bucket = Mock(BucketProxy)
        bucket.tryConsumeAndReturnRemaining(1) >> probe

        def builder = Mock(RemoteBucketBuilder)
        proxyManager.builder() >> builder

        when:
        rateLimiter.check("my-client")

        then:
        1 * builder.build("webhook:ratelimit:my-client", _ as Supplier) >> bucket
    }

    def "should calculate retryAfter as at least 1 second"() {
        given:
        def clientConfig = new WebhookClientsProperties.ClientConfig(true, "secret", 10)
        webhookClientsProperties.clients() >> Map.of("client-1", clientConfig)

        def probe = Mock(ConsumptionProbe)
        probe.isConsumed() >> false
        probe.getNanosToWaitForRefill() >> 1L // almost zero nanos

        def bucket = Mock(BucketProxy)
        bucket.tryConsumeAndReturnRemaining(1) >> probe

        def builder = Mock(RemoteBucketBuilder)
        builder.build(_ as String, _ as Supplier) >> bucket
        proxyManager.builder() >> builder

        when:
        rateLimiter.check("client-1")

        then:
        def ex = thrown(WebhookRateLimitExceededException)
        ex.message.contains("1 seconds")
    }
}
