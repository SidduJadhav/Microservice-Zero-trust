package com.zerotrust.order.client;

import com.zerotrust.order.model.InventoryRequest;
import com.zerotrust.order.model.InventoryResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

@Slf4j
@Component
@RequiredArgsConstructor
public class InventoryServiceClient {

    private final WebClient webClient;

    @Value("${services.inventory.url}")
    private String inventoryServiceUrl;

    public InventoryResponse checkInventory(InventoryRequest request) {
        log.info("[ORDER→INVENTORY] Checking stock for itemId={}, qty={}",
                request.getItemId(), request.getQuantity());

        return webClient.post()
                .uri(inventoryServiceUrl + "/api/inventory/check")
                .bodyValue(request)
                .retrieve()
                .bodyToMono(InventoryResponse.class)
                .doOnError(e -> log.error("[ORDER→INVENTORY] Call failed: {}", e.getMessage()))
                .block();
    }
}