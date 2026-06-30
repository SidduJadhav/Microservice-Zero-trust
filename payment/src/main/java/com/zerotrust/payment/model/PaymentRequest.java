package com.zerotrust.payment.model;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;
import lombok.Data;

@Data
public class PaymentRequest {

    @NotBlank(message = "Order ID is required")
    private String orderId;

    @NotBlank(message = "Customer ID is required")
    private String customerId;

    // ⚠️  DEMO: Sensitive fields — arrive in plaintext over HTTP in Scenario 1 & 2
    // Scenario 1: tcpdump captures these on the wire
    // Scenario 2: Rogue Service logs these and returns fake APPROVED
    // Scenario 3: mTLS encrypts transit + rejects Rogue (SSLHandshakeException)
    @NotBlank(message = "Card number is required")
    private String cardNumber;

    @NotBlank(message = "CVV is required")
    private String cvv;

    @NotBlank(message = "Expiry date is required")
    private String expiryDate;

    @Positive(message = "Amount must be positive")
    private double amount;
}