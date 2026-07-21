package com.rzodeczko.presentation.controller

import com.rzodeczko.IntegrationSpec
import com.rzodeczko.application.port.output.TaxSystemPort
import com.rzodeczko.domain.model.Invoice
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc
import org.spockframework.spring.SpringBean
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest
import org.springframework.http.MediaType
import org.springframework.test.web.servlet.MockMvc
import tools.jackson.databind.ObjectMapper

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*

@AutoConfigureMockMvc
class InvoiceControllerIT extends IntegrationSpec {

    @Autowired
    MockMvc mockMvc

    @SpringBean
    TaxSystemPort taxSystemPort = Mock()

    ObjectMapper objectMapper = new ObjectMapper()

    def "POST /invoices should return 201 when valid request"() {
        given:
        def externalId = "ext-123"
        taxSystemPort.issueInvoice(_ as Invoice) >> externalId

        and:
        def request = [
                orderId  : UUID.randomUUID().toString(),
                taxId    : "PL1234567890",
                buyerName: "Test Buyer Sp. z o.o.",
                items    : [
                        [name: "Consulting", quantity: 1, price: 100.00, taxRate: 23]
                ]
        ]

        when:
        def result = mockMvc.perform(post("/invoices")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))

        then:
        result.andExpect(status().isCreated())
                .andExpect(jsonPath('$.invoiceId').exists())
                .andExpect(jsonPath('$.status').value("ISSUED"))
    }

    def "POST /invoices should return 400 when orderId is missing"() {
        given:
        def request = [
                taxId    : "PL1234567890",
                buyerName: "Buyer",
                items    : [
                        [name: "Item", quantity: 1, price: 10.00, taxRate: 23]
                ]
        ]

        when:
        def result = mockMvc.perform(post("/invoices")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))

        then:
        result.andExpect(status().isBadRequest())
    }

    def "POST /invoices should return 400 when items list is empty"() {
        given:
        def request = [
                orderId  : UUID.randomUUID().toString(),
                taxId    : "PL1234567890",
                buyerName: "Buyer",
                items    : []
        ]

        when:
        def result = mockMvc.perform(post("/invoices")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))

        then:
        result.andExpect(status().isBadRequest())
    }

    def "POST /invoices should return 400 when buyerName is blank"() {
        given:
        def request = [
                orderId  : UUID.randomUUID().toString(),
                taxId    : "PL1234567890",
                buyerName: "",
                items    : [
                        [name: "Item", quantity: 1, price: 10.00, taxRate: 23]
                ]
        ]

        when:
        def result = mockMvc.perform(post("/invoices")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))

        then:
        result.andExpect(status().isBadRequest())
    }

    def "POST /invoices should return 409 when invoice for same orderId already exists"() {
        given:
        taxSystemPort.issueInvoice(_) >> "ext-first"

        and:
        def orderId = UUID.randomUUID().toString()
        def request = [
                orderId  : orderId,
                taxId    : "PL1234567890",
                buyerName: "Buyer",
                items    : [
                        [name: "Item", quantity: 1, price: 10.00, taxRate: 23]
                ]
        ]

        and: "first request creates the invoice"
        mockMvc.perform(post("/invoices")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())

        when: "second request with same orderId"
        def result = mockMvc.perform(post("/invoices")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))

        then: "should return 409 Conflict"
        result.andExpect(status().isConflict())
                .andExpect(jsonPath('$.status').value("409"))
    }

    def "GET /invoices/{id}/pdf should return 404 when invoice not found"() {
        when:
        def result = mockMvc.perform(get("/invoices/${UUID.randomUUID()}/pdf"))

        then:
        result.andExpect(status().isNotFound())
    }

    def "GET /invoices/{id}/pdf should return PDF when invoice is issued"() {
        given:
        def externalId = "ext-pdf-test"
        byte[] pdfBytes = [0x25, 0x50, 0x44, 0x46] as byte[]
        taxSystemPort.issueInvoice(_) >> externalId
        taxSystemPort.getPdf(externalId) >> pdfBytes

        and: "create and issue invoice first"
        def orderId = UUID.randomUUID().toString()
        def request = [
                orderId  : orderId,
                taxId    : "PL1234567890",
                buyerName: "Buyer",
                items    : [
                        [name: "Item", quantity: 1, price: 10.00, taxRate: 23]
                ]
        ]
        def createResult = mockMvc.perform(post("/invoices")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andReturn()

        def invoiceId = new tools.jackson.databind.ObjectMapper()
                .readTree(createResult.response.contentAsString)
                .get("invoiceId").asText()

        when:
        def result = mockMvc.perform(get("/invoices/${invoiceId}/pdf"))

        then:
        result.andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_PDF))
    }

    def "GET unknown endpoint should return 404"() {
        when:
        def result = mockMvc.perform(get("/unknown-endpoint"))

        then:
        result.andExpect(status().isNotFound())
    }
}
