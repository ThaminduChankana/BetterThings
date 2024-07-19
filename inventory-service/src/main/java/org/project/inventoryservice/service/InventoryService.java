package org.project.inventoryservice.service;

import lombok.RequiredArgsConstructor;
import org.modelmapper.ModelMapper;
import org.project.inventoryservice.dto.InventoryResponse;
import org.project.inventoryservice.model.Inventory;
import org.project.inventoryservice.repository.InventoryRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class InventoryService {

    private final InventoryRepository inventoryRepository;
    private final ModelMapper modelMapper;

    @Transactional(readOnly = true)
    public List<InventoryResponse> isInStock(List<String> skuCodes) {
        List<Inventory> inventoryList = inventoryRepository.findBySkuCodeIn(skuCodes);
        return skuCodes.stream()
                .map(skuCode -> inventoryList.stream()
                        .filter(inventory -> inventory.getSkuCode().equals(skuCode))
                        .findFirst()
                        .map(inventory -> {
                            InventoryResponse response = modelMapper.map(inventory, InventoryResponse.class);
                            response.setInStock(inventory.getQuantity() > 0);
                            return response;
                        })
                        .orElseGet(() -> InventoryResponse.builder()
                                .skuCode(skuCode)
                                .isInStock(false)
                                .build()))
                .collect(Collectors.toList());
    }
}
