package com.zerotrust.order.model;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class OrderResponse {
    private String orderId;
    private String status;          // SUCCESS / FAILED
    private String customerId;
    private String itemId;
    private int quantity;
    private String paymentStatus;
    private String inventoryStatus;
    private String message;
}