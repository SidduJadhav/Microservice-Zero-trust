package com.zerotrust.gateway.model;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;
import lombok.Data;

@Data
public class OrderRequest {

    @NotBlank(message = "Customer ID is required")
    private String customerId;

    @NotBlank(message = "Item ID is required")
    private String itemId;

    @Positive(message = "Quantity must be positive")
    private int quantity;

    // Sensitive payment data — forwarded as-is to Order Service
    // Intentionally plaintext for Scenario 1 & 2 demo
    @NotBlank(message = "Card number is required")
    private String cardNumber;

    @NotBlank(message = "CVV is required")
    private String cvv;

    @NotBlank(message = "Expiry date is required")
    private String expiryDate;
}