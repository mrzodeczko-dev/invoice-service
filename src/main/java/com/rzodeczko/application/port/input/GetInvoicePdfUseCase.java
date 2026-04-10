package com.rzodeczko.application.port.input;
import java.util.UUID;

public interface GetInvoicePdfUseCase {
    /*
    Zwraca byte[] - OK dla faktur (najczesciej max kilkaset KB)
    Dla plikow > 1MB mozesz rozwazyc InputStream (streaming bez buforowania w RAM).
    W 99% przypadkow dla faktur podejscie z byte[] jest ok.
     */
    byte[] getPdf(UUID invoiceId);
}
