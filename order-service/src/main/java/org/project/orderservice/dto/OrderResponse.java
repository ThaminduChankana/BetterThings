package org.project.orderservice.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class OrderResponse {
    private String orderNumber;
    private List<OrderLineItemsDTO> orderLineItemsList;

    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    public static class OrderLineItemsDTO {
        private String skuCode;
        private BigDecimal price;
        private Integer quantity;
    }

    private String responseCode;
    private String message;
}
