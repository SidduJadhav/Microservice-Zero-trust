package com.zerotrust.rogue.model;

import lombok.Data;

@Data
public class PaymentRequest {
    // Mirrors Payment Service model exactly —
    // so Order Service JSON deserializes without any issue
    private String orderId;
    private String customerId;
    private String cardNumber;   // ← stolen here in Scenario 2
    private String cvv;          // ← stolen here in Scenario 2
    private String expiryDate;
    private double amount;
}