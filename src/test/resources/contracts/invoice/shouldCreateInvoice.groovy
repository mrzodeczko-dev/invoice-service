package contracts.invoice

import org.springframework.cloud.contract.spec.Contract

Contract.make {
    description "Should create an invoice and return 201 Created"

    request {
        method POST()
        url "/invoices"
        headers {
            contentType applicationJson()
        }
        body(
                orderId: $(anyUuid()),
                taxId: "PL1234567890",
                buyerName: "John Doe",
                items: [
                        [
                                name    : "Gaming Laptop",
                                quantity: 1,
                                price   : 4999.99,
                                taxRate : 23
                        ]
                ]
        )
    }

    response {
        status CREATED()
        headers {
            contentType applicationJson()
        }
        body(
                invoiceId: $(anyUuid()),
                status: "ISSUED",
                message: $(anyNonBlankString())
        )
    }
}
