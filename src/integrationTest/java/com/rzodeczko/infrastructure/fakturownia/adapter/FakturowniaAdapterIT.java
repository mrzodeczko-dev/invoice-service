package com.rzodeczko.infrastructure.fakturownia.adapter;

import com.github.tomakehurst.wiremock.junit5.WireMockExtension;
import com.rzodeczko.application.exception.TaxSystemPermanentException;
import com.rzodeczko.application.exception.TaxSystemTemporaryException;
import com.rzodeczko.domain.model.Invoice;
import com.rzodeczko.domain.model.InvoiceItem;
import com.rzodeczko.domain.vo.TaxRate;
import com.rzodeczko.infrastructure.configuration.properties.FakturowniaProperties;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.web.client.RestClient;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class FakturowniaAdapterIT {

    @RegisterExtension
    static WireMockExtension wireMock = WireMockExtension.newInstance()
            .options(wireMockConfig().dynamicPort())
            .build();

    FakturowniaAdapter adapter;

    @BeforeEach
    void setup() {
        adapter = new FakturowniaAdapter(RestClient.builder(),
                new FakturowniaProperties("http://localhost:" + wireMock.getPort(), "test-token"));
    }

    // -------------------------------------------------------------------------
    // issueInvoice
    // -------------------------------------------------------------------------

    @Test
    void issueInvoice_shouldReturnExternalId_onSuccess() {
        wireMock.stubFor(post(urlPathEqualTo("/invoices.json"))
                .withQueryParam("api_token", equalTo("test-token"))
                .willReturn(aResponse()
                        .withStatus(201)
                        .withHeader("Content-Type", "application/json")
                        .withBody("{\"id\": 42}")));

        var result = adapter.issueInvoice(buildInvoice());

        assertThat(result).isEqualTo("42");
        wireMock.verify(1, postRequestedFor(urlPathEqualTo("/invoices.json")));
    }

    @Test
    void issueInvoice_shouldSendCorrectJsonBody() {
        wireMock.stubFor(post(urlPathEqualTo("/invoices.json"))
                .willReturn(aResponse()
                        .withStatus(201)
                        .withHeader("Content-Type", "application/json")
                        .withBody("{\"id\": 99}")));

        var orderId = UUID.randomUUID();
        var invoice = new Invoice(UUID.randomUUID(), orderId, "PL1234567890", "Test Buyer",
                List.of(new InvoiceItem("Widget", 2, new BigDecimal("15.00"), TaxRate.of(23))));

        adapter.issueInvoice(invoice);

        wireMock.verify(postRequestedFor(urlPathEqualTo("/invoices.json"))
                .withRequestBody(matchingJsonPath("$.invoice.kind", equalTo("vat")))
                .withRequestBody(matchingJsonPath("$.invoice.buyer_name", equalTo("Test Buyer")))
                .withRequestBody(matchingJsonPath("$.invoice.buyer_tax_no", equalTo("PL1234567890")))
                .withRequestBody(matchingJsonPath("$.invoice.oid", equalTo(orderId.toString())))
                .withRequestBody(matchingJsonPath("$.invoice.positions[0].name", equalTo("Widget")))
                .withRequestBody(matchingJsonPath("$.invoice.positions[0].quantity", equalTo("2")))
                .withRequestBody(matchingJsonPath("$.invoice.positions[0].total_price_gross", equalTo("30.0")))
                .withRequestBody(matchingJsonPath("$.invoice.positions[0].tax", equalTo("23"))));
    }

    @Test
    void issueInvoice_shouldThrowTemporaryException_whenIdIsNullInResponse() {
        wireMock.stubFor(post(urlPathEqualTo("/invoices.json"))
                .willReturn(aResponse()
                        .withStatus(201)
                        .withHeader("Content-Type", "application/json")
                        .withBody("{\"id\": null}")));

        assertThatThrownBy(() -> adapter.issueInvoice(buildInvoice()))
                .isInstanceOf(TaxSystemTemporaryException.class);
    }

    @ParameterizedTest
    @ValueSource(ints = {400, 401, 403, 404, 405, 409, 422})
    void issueInvoice_shouldThrowPermanentException_forClientErrorStatus(int status) {
        wireMock.stubFor(post(urlPathEqualTo("/invoices.json"))
                .willReturn(aResponse().withStatus(status)));

        assertThatThrownBy(() -> adapter.issueInvoice(buildInvoice()))
                .isInstanceOf(TaxSystemPermanentException.class);
    }

    @ParameterizedTest
    @ValueSource(ints = {408, 429, 500, 502, 503, 504})
    void issueInvoice_shouldThrowTemporaryException_forRetryableStatus(int status) {
        wireMock.stubFor(post(urlPathEqualTo("/invoices.json"))
                .willReturn(aResponse().withStatus(status)));

        assertThatThrownBy(() -> adapter.issueInvoice(buildInvoice()))
                .isInstanceOf(TaxSystemTemporaryException.class);
    }

    @Test
    void issueInvoice_shouldThrowTemporaryException_onConnectionFailure() {
        var deadAdapter = new FakturowniaAdapter(RestClient.builder(),
                new FakturowniaProperties("http://localhost:1", "test-token"));

        assertThatThrownBy(() -> deadAdapter.issueInvoice(buildInvoice()))
                .isInstanceOf(TaxSystemTemporaryException.class);
    }

    // -------------------------------------------------------------------------
    // getPdf
    // -------------------------------------------------------------------------

    @Test
    void getPdf_shouldReturnByteArray_onSuccess() {
        byte[] pdfBytes = {0x25, 0x50, 0x44, 0x46}; // %PDF
        wireMock.stubFor(get(urlPathEqualTo("/invoices/123.pdf"))
                .withQueryParam("api_token", equalTo("test-token"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/pdf")
                        .withBody(pdfBytes)));

        var result = adapter.getPdf("123");

        assertThat(result).isEqualTo(pdfBytes);
        wireMock.verify(1, getRequestedFor(urlPathEqualTo("/invoices/123.pdf")));
    }

    @ParameterizedTest
    @ValueSource(ints = {400, 401, 403, 404, 422})
    void getPdf_shouldThrowPermanentException_forClientErrorStatus(int status) {
        wireMock.stubFor(get(urlPathMatching("/invoices/.*\\.pdf"))
                .willReturn(aResponse().withStatus(status)));

        assertThatThrownBy(() -> adapter.getPdf("ext-err"))
                .isInstanceOf(TaxSystemPermanentException.class);
    }

    @ParameterizedTest
    @ValueSource(ints = {408, 429, 500, 502, 503, 504})
    void getPdf_shouldThrowTemporaryException_forRetryableStatus(int status) {
        wireMock.stubFor(get(urlPathMatching("/invoices/.*\\.pdf"))
                .willReturn(aResponse().withStatus(status)));

        assertThatThrownBy(() -> adapter.getPdf("ext-err"))
                .isInstanceOf(TaxSystemTemporaryException.class);
    }

    @Test
    void getPdf_shouldThrowTemporaryException_onConnectionFailure() {
        var deadAdapter = new FakturowniaAdapter(RestClient.builder(),
                new FakturowniaProperties("http://localhost:1", "test-token"));

        assertThatThrownBy(() -> deadAdapter.getPdf("123"))
                .isInstanceOf(TaxSystemTemporaryException.class);
    }

    // -------------------------------------------------------------------------
    // findByOrderId
    // -------------------------------------------------------------------------

    @Test
    void findByOrderId_shouldReturnList_onSuccess() {
        var orderId = "order-abc";
        wireMock.stubFor(get(urlPathEqualTo("/invoices.json"))
                .withQueryParam("api_token", equalTo("test-token"))
                .withQueryParam("oid", equalTo(orderId))
                .withQueryParam("status", equalTo("issued"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody("""
                                [
                                  {"id": "10", "oid": "%s"},
                                  {"id": "11", "oid": "%s"}
                                ]
                                """.formatted(orderId, orderId))));

        var result = adapter.findByOrderId(orderId);

        assertThat(result).hasSize(2);
        assertThat(result.get(0).id()).isEqualTo("10");
        assertThat(result.get(0).orderId()).isEqualTo(orderId);
        assertThat(result.get(1).id()).isEqualTo("11");
    }

    @Test
    void findByOrderId_shouldReturnEmptyList_whenResponseIsEmptyArray() {
        wireMock.stubFor(get(urlPathEqualTo("/invoices.json"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody("[]")));

        assertThat(adapter.findByOrderId("order-xyz")).isEmpty();
    }

    @Test
    void findByOrderId_shouldReturnEmptyList_whenResponseBodyIsNull() {
        wireMock.stubFor(get(urlPathEqualTo("/invoices.json"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody("null")));

        assertThat(adapter.findByOrderId("order-null")).isEmpty();
    }

    @ParameterizedTest
    @ValueSource(ints = {400, 401, 403, 404, 422})
    void findByOrderId_shouldThrowPermanentException_forClientErrorStatus(int status) {
        wireMock.stubFor(get(urlPathEqualTo("/invoices.json"))
                .willReturn(aResponse().withStatus(status)));

        assertThatThrownBy(() -> adapter.findByOrderId("order-err"))
                .isInstanceOf(TaxSystemPermanentException.class);
    }

    @ParameterizedTest
    @ValueSource(ints = {408, 429, 500, 502, 503, 504})
    void findByOrderId_shouldThrowTemporaryException_forRetryableStatus(int status) {
        wireMock.stubFor(get(urlPathEqualTo("/invoices.json"))
                .willReturn(aResponse().withStatus(status)));

        assertThatThrownBy(() -> adapter.findByOrderId("order-err"))
                .isInstanceOf(TaxSystemTemporaryException.class);
    }

    @Test
    void findByOrderId_shouldThrowTemporaryException_onConnectionFailure() {
        var deadAdapter = new FakturowniaAdapter(RestClient.builder(),
                new FakturowniaProperties("http://localhost:1", "test-token"));

        assertThatThrownBy(() -> deadAdapter.findByOrderId("order-abc"))
                .isInstanceOf(TaxSystemTemporaryException.class);
    }

    // -------------------------------------------------------------------------
    // helpers
    // -------------------------------------------------------------------------

    private Invoice buildInvoice() {
        return new Invoice(
                UUID.randomUUID(),
                UUID.randomUUID(),
                "PL9999999999",
                "ACME Sp. z o.o.",
                List.of(new InvoiceItem("Service", 1, new BigDecimal("100.00"), TaxRate.of(23)))
        );
    }
}
