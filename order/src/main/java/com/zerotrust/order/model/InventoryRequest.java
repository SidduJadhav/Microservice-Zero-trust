package com.zerotrust.order.model;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class InventoryRequest {
    private String orderId;
    private String itemId;
    private int quantity;
}