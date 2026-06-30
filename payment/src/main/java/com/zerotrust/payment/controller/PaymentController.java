package com.zerotrust.payment.controller;

import com.zerotrust.payment.model.PaymentRequest;
import com.zerotrust.payment.model.PaymentResponse;
import com.zerotrust.payment.service.PaymentService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequestMapping("/api/payment")
@RequiredArgsConstructor
public class PaymentController {

    private final PaymentService paymentService;

    /**
     * POST /api/payment/process
     * Receives card data from Order Service.
     * In Scenario 2, the Rogue Service exposes this same endpoint
     * to intercept the card data before the real service sees it.
     */
    @PostMapping("/process")
    public ResponseEntity<PaymentResponse> processPayment(
            @Valid @RequestBody PaymentRequest request) {

        log.info("[CONTROLLER] Payment request received for orderId={}", request.getOrderId());
        PaymentResponse response = paymentService.processPayment(request);
        return ResponseEntity.ok(response);
    }

    /** Health check */
    @GetMapping("/health")
    public ResponseEntity<String> health() {
        return ResponseEntity.ok("Payment Service is UP — LEGITIMATE");
    }
}