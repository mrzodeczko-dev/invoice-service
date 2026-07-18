package com.rzodeczko.infrastructure.webhook.access

import spock.lang.Specification
import spock.lang.Unroll

class TrustedWebhookClientSpec extends Specification {

    def "from should return FAKTUROWNIA for 'fakturownia'"() {
        when:
        def result = TrustedWebhookClient.from("fakturownia")

        then:
        result.isPresent()
        result.get() == TrustedWebhookClient.FAKTUROWNIA
        result.get().clientId == "fakturownia"
    }

    @Unroll
    def "from should return empty for unknown client '#clientId'"() {
        expect:
        TrustedWebhookClient.from(clientId).isEmpty()

        where:
        clientId << ["unknown", "FAKTUROWNIA", "Fakturownia", "", null]
    }
}
