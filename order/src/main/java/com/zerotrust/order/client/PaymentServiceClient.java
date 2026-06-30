package com.zerotrust.order.client;

import com.zerotrust.order.model.PaymentRequest;
import com.zerotrust.order.model.PaymentResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

@Slf4j
@Component
@RequiredArgsConstructor
public class PaymentServiceClient {

    private final WebClient webClient;

    @Value("${services.payment.url}")
    private String paymentServiceUrl;

    public PaymentResponse processPayment(PaymentRequest request) {
        log.info("[ORDER→PAYMENT] Sending payment request for orderId={}, customerId={}, card={}",
                request.getOrderId(),
                request.getCustomerId(),
                maskCard(request.getCardNumber()));

        // NOTE: In Scenario 1 (plain HTTP), this goes over the wire unencrypted.
        // Wireshark / tcpdump will capture the raw cardNumber + CVV.
        return webClient.post()
                .uri(paymentServiceUrl + "/api/payment/process")
                .bodyValue(request)
                .retrieve()
                .bodyToMono(PaymentResponse.class)
                .doOnError(e -> log.error("[ORDER→PAYMENT] Call failed: {}", e.getMessage()))
                .block();
    }

    private String maskCard(String cardNumber) {
        if (cardNumber == null || cardNumber.length() < 4) return "****";
        return "**** **** **** " + cardNumber.substring(cardNumber.length() - 4);
    }
}