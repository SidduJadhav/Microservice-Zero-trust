package com.zerotrust.payment.service;

import com.zerotrust.payment.model.PaymentRequest;
import com.zerotrust.payment.model.PaymentResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Slf4j
@Service
public class PaymentService {

    public PaymentResponse processPayment(PaymentRequest request) {

        // ⚠️  DEMO LOG — intentionally prints sensitive data to show the attack impact
        // In Scenario 1 & 2, this data arrived unencrypted / via Rogue Service
        log.warn("===========================================================");
        log.warn("[PAYMENT SERVICE] Received sensitive payment data:");
        log.warn("  orderId    = {}", request.getOrderId());
        log.warn("  customerId = {}", request.getCustomerId());
        log.warn("  cardNumber = {}", request.getCardNumber());   // visible in tcpdump Scenario 1
        log.warn("  cvv        = {}", request.getCvv());
        log.warn("  expiry     = {}", request.getExpiryDate());
        log.warn("  amount     = ${}", request.getAmount());
        log.warn("===========================================================");

        // Stub approval logic — always APPROVED for demo
        // (extend with DECLINED cases if needed for richer demo)
        String transactionId = "TXN-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();

        log.info("[PAYMENT SERVICE] Payment APPROVED — transactionId={}", transactionId);

        return PaymentResponse.builder()
                .transactionId(transactionId)
                .orderId(request.getOrderId())
                .customerId(request.getCustomerId())
                .status("APPROVED")
                .amount(request.getAmount())
                .message("Payment processed successfully by LEGITIMATE payment-service")
                .build();
    }
}