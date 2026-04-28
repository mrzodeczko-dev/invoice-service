package com.rzodeczko.infrastructure.fakturownia.adapter;

import com.rzodeczko.application.exception.TaxSystemPermanentException;
import com.rzodeczko.application.exception.TaxSystemTemporaryException;
import com.rzodeczko.application.port.output.TaxSystemPort;
import com.rzodeczko.domain.model.Invoice;
import com.rzodeczko.infrastructure.configuration.properties.FakturowniaProperties;
import com.rzodeczko.infrastructure.fakturownia.dto.CreateInvoiceDto;
import com.rzodeczko.infrastructure.fakturownia.dto.CreateInvoiceWrapperDto;
import com.rzodeczko.infrastructure.fakturownia.dto.FakturowniaCreateInvoiceResponseDto;
import com.rzodeczko.infrastructure.fakturownia.dto.PositionDto;
import com.rzodeczko.presentation.dto.FakturowniaGetInvoiceDto;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.stereotype.Component;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Component
public class FakturowniaAdapter implements TaxSystemPort {

    private final RestClient restClient;
    private final FakturowniaProperties fakturowniaProperties;

    public FakturowniaAdapter(
            RestClient.Builder restClientBuilder,
            FakturowniaProperties fakturowniaProperties
    ) {
        this.fakturowniaProperties = fakturowniaProperties;
        this.restClient = restClientBuilder
                .baseUrl(fakturowniaProperties.url())
                .build();
    }

    @Override
    public String issueInvoice(Invoice invoice) {
        try {
            var createResponse = restClient.post()
                    .uri(uri -> uri
                            .path("/invoices.json")
                            .queryParam("api_token", fakturowniaProperties.token())
                            .build())
                    .body(mapToRequest(invoice))
                    .retrieve()
                    .onStatus(HttpStatusCode::isError,
                            (request, clientResponse) -> handleError(
                                    clientResponse.getStatusCode(),
                                    "issueInvoice",
                                    "orderId=" + invoice.getOrderId()
                            ))
                    .body(FakturowniaCreateInvoiceResponseDto.class);

            if (createResponse == null || createResponse.id() == null) {
                throw new TaxSystemTemporaryException(
                        "Fakturownia returned empty invoice ID. orderId=" + invoice.getOrderId()
                );
            }

            return String.valueOf(createResponse.id());
        } catch (ResourceAccessException e) {
            throw new TaxSystemTemporaryException(
                    "Timeout/connection error during issueInvoice. orderId=" + invoice.getOrderId(), e
            );
        } catch (RestClientException e) {
            throw new TaxSystemTemporaryException(
                    "Rest client error during issueInvoice. orderId=" + invoice.getOrderId(), e
            );
        }
    }

    @Override
    public byte[] getPdf(String externalId) {
        try {
            return restClient.get()
                    .uri(uri -> uri
                            .path("/invoices/{id}.pdf")
                            .queryParam("api_token", fakturowniaProperties.token())
                            .build(externalId))
                    .retrieve()
                    .onStatus(HttpStatusCode::isError,
                            (request, response) -> handleError(
                                    response.getStatusCode(),
                                    "getPdf",
                                    "externalId=" + externalId
                            ))
                    .body(byte[].class);
        } catch (TaxSystemPermanentException | TaxSystemTemporaryException e) {
            throw e;
        } catch (ResourceAccessException e) {
            throw new TaxSystemTemporaryException(
                    "Timeout/connection error during getPdf. externalId=" + externalId, e
            );
        } catch (RestClientException e) {
            throw new TaxSystemTemporaryException(
                    "Rest client error during getPdf. externalId=" + externalId, e
            );
        }
    }

    @Override
    public List<FakturowniaGetInvoiceDto> findByOrderId(String orderId) {
        try {
            List<FakturowniaGetInvoiceDto> invoicesResponse = restClient.get()
                    .uri(uri -> uri
                            .path("/invoices.json")
                            .queryParam("api_token", fakturowniaProperties.token())
                            .queryParam("oid", orderId)
                            .queryParam("status", "issued")
                            .build())
                    .retrieve()
                    .onStatus(HttpStatusCode::isError,
                            (request, clientResponse) -> handleError(
                                    clientResponse.getStatusCode(),
                                    "findByOrderId",
                                    "orderId=" + orderId
                            ))
                    .body(new ParameterizedTypeReference<>() {
                    });

            return invoicesResponse == null ? List.of() : invoicesResponse;
        } catch (ResourceAccessException e) {
            throw new TaxSystemTemporaryException(
                    "Timeout/connection error during findByOrderId. orderId=" + orderId, e
            );
        } catch (RestClientException e) {
            throw new TaxSystemTemporaryException(
                    "Rest client error during findByOrderId. orderId=" + orderId, e
            );
        }
    }

    private void handleError(HttpStatusCode statusCode, String operation, String context) {
        if (!(statusCode instanceof HttpStatus status)) {
            throw new TaxSystemTemporaryException(
                    "Unknown HTTP status during " + operation + ". " + context + ", status=" + statusCode
            );
        }

        switch (status) {
            case BAD_REQUEST,
                 UNAUTHORIZED,
                 FORBIDDEN,
                 NOT_FOUND,
                 METHOD_NOT_ALLOWED,
                 UNPROCESSABLE_CONTENT -> throw new TaxSystemPermanentException(
                    "Permanent Fakturownia error during " + operation + ". " + context + ", status=" + status
            );

            case REQUEST_TIMEOUT,
                 TOO_MANY_REQUESTS -> throw new TaxSystemTemporaryException(
                    "Retryable Fakturownia client-side error during " + operation + ". " + context + ", status=" + status
            );

            case CONFLICT -> throw new TaxSystemPermanentException(
                    "Conflict during " + operation + ". " + context + ", status=" + status
            );

            case INTERNAL_SERVER_ERROR,
                 BAD_GATEWAY,
                 SERVICE_UNAVAILABLE,
                 GATEWAY_TIMEOUT -> throw new TaxSystemTemporaryException(
                    "Temporary Fakturownia server error during " + operation + ". " + context + ", status=" + status
            );

            default -> {
                if (status.is4xxClientError()) {
                    throw new TaxSystemPermanentException(
                            "Unhandled 4xx during " + operation + ". " + context + ", status=" + status
                    );
                }

                if (status.is5xxServerError()) {
                    throw new TaxSystemTemporaryException(
                            "Unhandled 5xx during " + operation + ". " + context + ", status=" + status
                    );
                }

                throw new TaxSystemTemporaryException(
                        "Unexpected HTTP status during " + operation + ". " + context + ", status=" + status
                );
            }
        }
    }

    private CreateInvoiceWrapperDto mapToRequest(Invoice invoice) {
        LocalDate now = LocalDate.now();

        List<PositionDto> positions = invoice.getItems().stream()
                .map(item -> new PositionDto(
                        item.name(),
                        item.taxRate().intValue(),
                        item.quantity(),
                        item.unitPrice().multiply(BigDecimal.valueOf(item.quantity()))
                ))
                .toList();

        return new CreateInvoiceWrapperDto(new CreateInvoiceDto(
                "vat",
                now.toString(),
                now.toString(),
                now.plusDays(7).toString(),
                invoice.getBuyerName(),
                invoice.getTaxId(),
                invoice.getOrderId().toString(),
                positions
        ));
    }
}