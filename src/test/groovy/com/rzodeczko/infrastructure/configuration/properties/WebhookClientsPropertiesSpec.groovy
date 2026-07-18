package com.rzodeczko.infrastructure.configuration.properties

import spock.lang.Specification

class WebhookClientsPropertiesSpec extends Specification {

    def "null clients map should default to empty"() {
        when:
        def props = new WebhookClientsProperties(null)

        then:
        props.clients().isEmpty()
    }

    def "clients map should be immutable"() {
        given:
        def mutable = new HashMap<String, WebhookClientsProperties.ClientConfig>()
        mutable.put("app", new WebhookClientsProperties.ClientConfig(true, "secret", 60))

        when:
        def props = new WebhookClientsProperties(mutable)
        props.clients().put("new-app", new WebhookClientsProperties.ClientConfig(true, "s", 10))

        then:
        thrown(UnsupportedOperationException)
    }

    def "ClientConfig enabled should default to true when null"() {
        when:
        def config = new WebhookClientsProperties.ClientConfig(null, "secret", 60)

        then:
        config.enabled()
    }

    def "ClientConfig enabled should stay false when set to false"() {
        when:
        def config = new WebhookClientsProperties.ClientConfig(false, "secret", 60)

        then:
        !config.enabled()
    }

    def "ClientConfig sharedSecret should default to empty string when null"() {
        when:
        def config = new WebhookClientsProperties.ClientConfig(true, null, 60)

        then:
        config.sharedSecret() == ""
    }

    def "ClientConfig should preserve sharedSecret when provided"() {
        when:
        def config = new WebhookClientsProperties.ClientConfig(true, "my-secret", 60)

        then:
        config.sharedSecret() == "my-secret"
    }

    def "ClientConfig should preserve requestsPerMinuteLimit"() {
        when:
        def config = new WebhookClientsProperties.ClientConfig(true, "s", 120)

        then:
        config.requestsPerMinuteLimit() == 120
    }
}
