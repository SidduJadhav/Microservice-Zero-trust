package com.zerotrust.order.service;

import com.zerotrust.order.client.InventoryServiceClient;
import com.zerotrust.order.client.PaymentServiceClient;
import com.zerotrust.order.model.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class OrderService {

    private final PaymentServiceClient paymentClient;
    private final InventoryServiceClient inventoryClient;

    public OrderResponse placeOrder(OrderRequest request) {
        String orderId = UUID.randomUUID().toString();
        log.info("=== [ORDER SERVICE] Processing orderId={} for customerId={} ===",
                orderId, request.getCustomerId());

        // Step 1 — Check Inventory
        InventoryResponse inventory = inventoryClient.checkInventory(
                InventoryRequest.builder()
                        .orderId(orderId)
                        .itemId(request.getItemId())
                        .quantity(request.getQuantity())
                        .build()
        );

        if (inventory == null || !inventory.isAvailable()) {
            log.warn("[ORDER] Inventory check FAILED for itemId={}", request.getItemId());
            return OrderResponse.builder()
                    .orderId(orderId)
                    .status("FAILED")
                    .customerId(request.getCustomerId())
                    .itemId(request.getItemId())
                    .quantity(request.getQuantity())
                    .inventoryStatus("OUT_OF_STOCK")
                    .paymentStatus("NOT_ATTEMPTED")
                    .message("Item out of stock.")
                    .build();
        }

        log.info("[ORDER] Inventory OK. remainingStock={}", inventory.getRemainingStock());

        // Step 2 — Process Payment
        // ⚠️  DEMO: cardNumber + CVV forwarded here.
        //    Scenario 1 → visible in tcpdump (plain HTTP)
        //    Scenario 2 → intercepted by Rogue Service
        //    Scenario 3 → encrypted + Rogue rejected (mTLS)
        PaymentResponse payment = paymentClient.processPayment(
                PaymentRequest.builder()
                        .orderId(orderId)
                        .customerId(request.getCustomerId())
                        .cardNumber(request.getCardNumber())
                        .cvv(request.getCvv())
                        .expiryDate(request.getExpiryDate())
                        .amount(calculateAmount(request.getQuantity()))
                        .build()
        );

        if (payment == null || !"APPROVED".equalsIgnoreCase(payment.getStatus())) {
            log.warn("[ORDER] Payment DECLINED for orderId={}", orderId);
            return OrderResponse.builder()
                    .orderId(orderId)
                    .status("FAILED")
                    .customerId(request.getCustomerId())
                    .itemId(request.getItemId())
                    .quantity(request.getQuantity())
                    .inventoryStatus("RESERVED")
                    .paymentStatus("DECLINED")
                    .message("Payment declined: " + (payment != null ? payment.getMessage() : "no response"))
                    .build();
        }

        log.info("[ORDER] Payment APPROVED. transactionId={}", payment.getTransactionId());

        return OrderResponse.builder()
                .orderId(orderId)
                .status("SUCCESS")
                .customerId(request.getCustomerId())
                .itemId(request.getItemId())
                .quantity(request.getQuantity())
                .inventoryStatus("RESERVED")
                .paymentStatus("APPROVED")
                .message("Order placed successfully. transactionId=" + payment.getTransactionId())
                .build();
    }

    private double calculateAmount(int quantity) {
        // Stub — fixed unit price $49.99 for demo
        return quantity * 49.99;
    }
}