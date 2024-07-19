package org.project.orderservice.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.modelmapper.ModelMapper;
import org.project.orderservice.dto.InventoryResponse;
import org.project.orderservice.dto.OrderRequest;
import org.project.orderservice.dto.OrderResponse;
import org.project.orderservice.model.Order;
import org.project.orderservice.model.OrderLineItems;
import org.project.orderservice.repository.OrderRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class OrderService {

    private final OrderRepository orderRepository;
    private final ModelMapper modelMapper;
    private final WebClient webClient;

    public OrderResponse placeOrder(OrderRequest orderRequest) {
        Order order = new Order();

        List<OrderLineItems> orderLineItemsList = orderRequest.getOrderLineItemsDTOList().stream()
                .map(dto -> modelMapper.map(dto, OrderLineItems.class))
                .collect(Collectors.toList());

        order.setOrderNumber(UUID.randomUUID().toString());
        order.setOrderLineItemsList(orderLineItemsList);

        List<String> skuCodes = order.getOrderLineItemsList().stream()
                .map(OrderLineItems::getSkuCode)
                .collect(Collectors.toList());

        // Call inventory service to check if products are in stock
        List<InventoryResponse> inventoryResponses = webClient.get()
                .uri("http://localhost:8082/api/inventory",
                        uriBuilder -> uriBuilder.queryParam("skuCode", skuCodes).build())
                .retrieve()
                .bodyToFlux(InventoryResponse.class)
                .collectList()
                .block();

        if (inventoryResponses == null) {
            throw new RuntimeException("Inventory service returned null response");
        }

        boolean allProductsInStock = inventoryResponses.stream()
                .allMatch(InventoryResponse::isInStock);

        if (allProductsInStock) {
            Order savedOrder = orderRepository.save(order);
            return OrderResponse.builder()
                    .orderNumber(savedOrder.getOrderNumber())
                    .orderLineItemsList(orderRequest.getOrderLineItemsDTOList().stream()
                            .map(dto -> modelMapper.map(dto, OrderResponse.OrderLineItemsDTO.class))
                            .collect(Collectors.toList()))
                    .responseCode("00")
                    .message("Order successfully created")
                    .build();
        } else {
            // Find out which items are not in stock
            List<String> outOfStockItems = inventoryResponses.stream()
                    .filter(response -> !response.isInStock())
                    .map(InventoryResponse::getSkuCode)
                    .collect(Collectors.toList());

            // Filter out the order line items that are not in stock
            List<OrderResponse.OrderLineItemsDTO> unavailableOrderLineItems = orderRequest.getOrderLineItemsDTOList().stream()
                    .filter(item -> outOfStockItems.contains(item.getSkuCode()))
                    .map(item -> modelMapper.map(item, OrderResponse.OrderLineItemsDTO.class))
                    .collect(Collectors.toList());

            String message = "The following items are out of stock: " + String.join(", ", outOfStockItems);

            return OrderResponse.builder()
                    .orderNumber(UUID.randomUUID().toString()) // Or use the order number if applicable
                    .orderLineItemsList(unavailableOrderLineItems)
                    .responseCode("01")
                    .message(message)
                    .build();
        }
    }
}
