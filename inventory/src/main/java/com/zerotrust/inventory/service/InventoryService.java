package com.zerotrust.inventory.service;

import com.zerotrust.inventory.model.InventoryRequest;
import com.zerotrust.inventory.model.InventoryResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Service
public class InventoryService {

    // In-memory stock store — sufficient for demo purposes
    // Key = itemId, Value = stock count
    private final Map<String, Integer> stock = new ConcurrentHashMap<>(Map.of(
            "ITEM-001", 50,
            "ITEM-002", 20,
            "ITEM-003", 5,
            "ITEM-004", 0   // out of stock — useful for Scenario 1 demo variation
    ));


    public InventoryResponse checkInventory(InventoryRequest request) {
        log.info("[INVENTORY SERVICE] Stock check — itemId={}, requestedQty={}",
                request.getItemId(), request.getQuantity());

        Integer currentStock = stock.getOrDefault(request.getItemId(), 0);

        if (currentStock <= 0 || currentStock < request.getQuantity()) {
            log.warn("[INVENTORY SERVICE] OUT OF STOCK — itemId={}, stock={}, requested={}",
                    request.getItemId(), currentStock, request.getQuantity());

            return InventoryResponse.builder()
                    .itemId(request.getItemId())
                    .available(false)
                    .remainingStock(currentStock)
                    .message("Insufficient stock for itemId=" + request.getItemId())
                    .build();
        }

        // Reserve stock (deduct for the order)
        stock.put(request.getItemId(), currentStock - request.getQuantity());
        int remaining = stock.get(request.getItemId());

        log.info("[INVENTORY SERVICE] RESERVED {} units of itemId={}. remainingStock={}",
                request.getQuantity(), request.getItemId(), remaining);

        return InventoryResponse.builder()
                .itemId(request.getItemId())
                .available(true)
                .remainingStock(remaining)
                .message("Stock reserved successfully")
                .build();
    }
}