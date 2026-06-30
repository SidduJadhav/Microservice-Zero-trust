package com.zerotrust.order.model;

import lombok.Data;

@Data
public class InventoryResponse {
    private String itemId;
    private boolean available;
    private int remainingStock;
    private String message;
}