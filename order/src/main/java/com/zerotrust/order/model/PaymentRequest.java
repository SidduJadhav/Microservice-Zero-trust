package com.zerotrust.order.model;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class PaymentRequest {
    private String orderId;
    private String customerId;
    // Sensitive — forwarded to Payment Service (plaintext in Scenario 1 & 2)
    private String cardNumber;
    private String cvv;
    private String expiryDate;
    private double amount;
}