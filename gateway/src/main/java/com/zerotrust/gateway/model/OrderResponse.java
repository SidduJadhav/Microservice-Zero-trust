package com.zerotrust.gateway.model;

import lombok.Data;

@Data
public class OrderResponse {
    private String orderId;
    private String status;
    private String customerId;
    private String itemId;
    private int quantity;
    private String paymentStatus;
    private String inventoryStatus;
    private String message;
}