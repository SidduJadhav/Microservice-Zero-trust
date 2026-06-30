package com.zerotrust.payment.model;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class PaymentResponse {
    private String transactionId;
    private String orderId;
    private String customerId;
    private String status;      // APPROVED / DECLINED
    private double amount;
    private String message;
}