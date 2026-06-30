package com.zerotrust.inventory.model;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class InventoryResponse {
    private String itemId;
    private boolean available;
    private int remainingStock;
    private String message;
}