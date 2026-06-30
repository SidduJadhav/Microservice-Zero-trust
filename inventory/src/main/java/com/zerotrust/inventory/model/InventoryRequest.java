package com.zerotrust.inventory.model;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;
import lombok.Data;

@Data
public class InventoryRequest {

    @NotBlank(message = "Order ID is required")
    private String orderId;

    @NotBlank(message = "Item ID is required")
    private String itemId;

    @Positive(message = "Quantity must be positive")
    private int quantity;
}