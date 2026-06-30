package com.zerotrust.order.controller;

import com.zerotrust.order.model.OrderRequest;
import com.zerotrust.order.model.OrderResponse;
import com.zerotrust.order.service.OrderService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequestMapping("/api/orders")
@RequiredArgsConstructor
public class OrderController {

    private final OrderService orderService;

    /**
     * POST /api/orders/place
     * Main endpoint — receives order with sensitive card data.
     * Used across all 3 demo scenarios.
     */
    @PostMapping("/place")
    public ResponseEntity<OrderResponse> placeOrder(@Valid @RequestBody OrderRequest request) {
        log.info("[CONTROLLER] Received order for customerId={}", request.getCustomerId());
        OrderResponse response = orderService.placeOrder(request);
        return ResponseEntity.ok(response);
    }

    /** Health-check convenience endpoint */
    @GetMapping("/health")
    public ResponseEntity<String> health() {
        return ResponseEntity.ok("Order Service is UP");
    }
}