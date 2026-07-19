package com.rzodeczko.contract;

import com.rzodeczko.application.port.input.GenerateInvoiceCommand;
import com.rzodeczko.application.port.input.GenerateInvoiceUseCase;
import com.rzodeczko.application.port.input.GetInvoicePdfUseCase;
import com.rzodeczko.application.port.input.InvoiceIssueResult;
import com.rzodeczko.presentation.controller.InvoiceController;
import com.rzodeczko.presentation.exception.GlobalExceptionHandler;
import com.rzodeczko.presentation.mapper.CreateInvoiceResponseMapper;
import io.restassured.module.mockmvc.RestAssuredMockMvc;
import org.junit.jupiter.api.BeforeEach;

import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public abstract class BaseContractTest {

    private final GenerateInvoiceUseCase generateInvoiceUseCase = mock(GenerateInvoiceUseCase.class);
    private final GetInvoicePdfUseCase getInvoicePdfUseCase = mock(GetInvoicePdfUseCase.class);

    // Real mapper
    private final CreateInvoiceResponseMapper createInvoiceResponseMapper = new CreateInvoiceResponseMapper();

    @BeforeEach
    void setup() {
        when(generateInvoiceUseCase.generate(any(GenerateInvoiceCommand.class)))
                .thenReturn(new InvoiceIssueResult.Issued(UUID.randomUUID()));

        RestAssuredMockMvc.standaloneSetup(
                new InvoiceController(
                        generateInvoiceUseCase,
                        getInvoicePdfUseCase,
                        createInvoiceResponseMapper
                ),
                new GlobalExceptionHandler()
        );
    }
}
