package com.zerotrust.gateway.controller;

import com.zerotrust.gateway.model.OrderRequest;
import com.zerotrust.gateway.model.OrderResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;

@Slf4j
@RestController
@RequestMapping("/api/gateway")
@RequiredArgsConstructor
public class GatewayController {

    private final WebClient webClient;

    @Value("${services.order.url}")
    private String orderServiceUrl;

    /**
     * POST /api/gateway/order
     *
     * Single entry point for the entire demo.
     * Receives order + card data from the external client (Postman/curl),
     * forwards everything to Order Service.
     *
     * Scenario 1: plain HTTP  → tcpdump sees cardNumber + CVV
     * Scenario 2: DNS/K8s points payment-service → rogue → card stolen
     * Scenario 3: mTLS        → rogue rejected, traffic encrypted
     */
    @PostMapping("/order")
    public ResponseEntity<?> placeOrder(@Valid @RequestBody OrderRequest request) {
        log.info("=== [GATEWAY] Incoming order from customerId={} ===",
                request.getCustomerId());
        log.info("[GATEWAY] Forwarding to Order Service at {}", orderServiceUrl);

        try {
            OrderResponse response = webClient.post()
                    .uri(orderServiceUrl + "/api/orders/place")
                    .bodyValue(request)
                    .retrieve()
                    .bodyToMono(OrderResponse.class)
                    .block();

            log.info("[GATEWAY] Order result — status={}, orderId={}",
                    response != null ? response.getStatus() : "null",
                    response != null ? response.getOrderId() : "null");

            return ResponseEntity.ok(response);

        } catch (WebClientResponseException ex) {
            log.error("[GATEWAY] Downstream error — HTTP {} : {}",
                    ex.getStatusCode(), ex.getResponseBodyAsString());
            return ResponseEntity
                    .status(ex.getStatusCode())
                    .body("Downstream service error: " + ex.getMessage());

        } catch (Exception ex) {
            // In Scenario 3: SSLHandshakeException surfaces here when
            // Rogue Service fails mTLS — logged clearly for demo
            log.error("[GATEWAY] Call failed — {}: {}", ex.getClass().getSimpleName(), ex.getMessage());
            return ResponseEntity
                    .status(503)
                    .body("Service unavailable: " + ex.getClass().getSimpleName()
                            + " — " + ex.getMessage());
        }
    }

    /** Health check */
    @GetMapping("/health")
    public ResponseEntity<String> health() {
        return ResponseEntity.ok("API Gateway is UP");
    }
}