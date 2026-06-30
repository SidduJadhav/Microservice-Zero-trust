package com.zerotrust.inventory.controller;

import com.zerotrust.inventory.model.InventoryRequest;
import com.zerotrust.inventory.model.InventoryResponse;
import com.zerotrust.inventory.service.InventoryService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequestMapping("/api/inventory")
@RequiredArgsConstructor
public class InventoryController {

    private final InventoryService inventoryService;

    /**
     * POST /api/inventory/check
     * Called by Order Service to verify stock before payment.
     */
    @PostMapping("/check")
    public ResponseEntity<InventoryResponse> checkInventory(
            @Valid @RequestBody InventoryRequest request) {

        log.info("[CONTROLLER] Inventory check received for itemId={}", request.getItemId());
        InventoryResponse response = inventoryService.checkInventory(request);
        return ResponseEntity.ok(response);
    }

    /**
     * GET /api/inventory/stock/{itemId}
     * Convenience endpoint — inspect current stock during demo.
     */
    @GetMapping("/stock/{itemId}")
    public ResponseEntity<String> getStock(@PathVariable String itemId) {
        return ResponseEntity.ok("Inventory Service is UP — LEGITIMATE");
    }

    /** Health check */
    @GetMapping("/health")
    public ResponseEntity<String> health() {
        return ResponseEntity.ok("Inventory Service is UP");
    }
}