package com.rzodeczko.presentation.controller

import com.rzodeczko.IntegrationSpec
import com.rzodeczko.application.port.output.TaxSystemPort
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc
import org.spockframework.spring.SpringBean
import org.springframework.http.MediaType
import org.springframework.test.web.servlet.MockMvc
import tools.jackson.databind.ObjectMapper

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status

@AutoConfigureMockMvc
class WebhookControllerIT extends IntegrationSpec {

    @Autowired
    MockMvc mockMvc

    @SpringBean
    TaxSystemPort taxSystemPort = Mock(TaxSystemPort)

    ObjectMapper objectMapper = new ObjectMapper()

    def "should return 200 when payload has no externalId"() {
        given:
        def payload = [
                id       : 1,
                deal     : [external_ids: [:]],
                app_name : "fakturownia",
                api_token: "secret"
        ]

        when:
        def result = mockMvc.perform(post("/webhooks/fakturownia/invoices/update")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(payload)))

        then:
        result.andExpect(status().isOk())
    }

    def "should return 200 when payload has null deal"() {
        given:
        def payload = [
                id       : 1,
                deal     : null,
                app_name : "fakturownia",
                api_token: "secret"
        ]

        when:
        def result = mockMvc.perform(post("/webhooks/fakturownia/invoices/update")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(payload)))

        then:
        result.andExpect(status().isOk())
    }

    def "should return 200 and invoke use case when externalId is present"() {
        given:
        def payload = [
                id       : 1,
                deal     : [external_ids: [fakturownia: 456]],
                app_name : "fakturownia",
                api_token: "secret"
        ]

        when:
        def result = mockMvc.perform(post("/webhooks/fakturownia/invoices/update")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(payload)))

        then:
        result.andExpect(status().isOk())
    }

    def "should return 401 when app_name is not trusted"() {
        given:
        def payload = [
                id       : 1,
                deal     : [external_ids: [fakturownia: 123]],
                app_name : "unknown-app",
                api_token: "secret"
        ]

        when:
        def result = mockMvc.perform(post("/webhooks/fakturownia/invoices/update")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(payload)))

        then:
        result.andExpect(status().isUnauthorized())
    }

    def "should return 401 when api_token does not match shared secret"() {
        given:
        def payload = [
                id       : 1,
                deal     : [external_ids: [fakturownia: 123]],
                app_name : "fakturownia",
                api_token: "wrong-secret"
        ]

        when:
        def result = mockMvc.perform(post("/webhooks/fakturownia/invoices/update")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(payload)))

        then:
        result.andExpect(status().isUnauthorized())
    }
}
