package com.zerotrust.order.model;

import lombok.Data;

@Data
public class PaymentResponse {
    private String transactionId;
    private String status;      // APPROVED / DECLINED
    private String message;
}