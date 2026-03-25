package com.xyz.orders.service;

import com.xyz.orders.exception.InsufficientStockException;
import com.xyz.orders.exception.ResourceNotFoundException;
import com.xyz.orders.model.Inventory;
import com.xyz.orders.model.Product;
import com.xyz.orders.repository.InventoryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class InventoryService {

    private final InventoryRepository inventoryRepository;
    private final ProductService productService;

    public List<Inventory> findAll() {
        return this.inventoryRepository.findAll();
    }

    public Inventory findById(Long id) {
        return this.inventoryRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Inventory", id));
    }

    public Inventory findByProductId(Long productId) {
        return this.inventoryRepository.findByProductId(productId)
                .orElseThrow(() -> new ResourceNotFoundException("Inventory for product", productId));
    }

    @Transactional
    public Inventory initializeStock(Long productId, int quantity) {
        Product product = this.productService.findById(productId);
        Inventory inventory = Inventory.builder()
                .product(product)
                .quantity(quantity)
                .reservedQuantity(0)
                .build();
        return this.inventoryRepository.save(inventory);
    }

    @Transactional
    public Inventory addStock(Long productId, int quantity) {
        Inventory inventory = this.findByProductId(productId);
        inventory.setQuantity(inventory.getQuantity() + quantity);
        return this.inventoryRepository.save(inventory);
    }

    @Transactional
    public Inventory reserveStock(Long productId, int quantity) {
        Inventory inventory = this.findByProductId(productId);
        if (inventory.getAvailableQuantity() < quantity) {
            throw new InsufficientStockException(productId, quantity, inventory.getAvailableQuantity());
        }
        inventory.setReservedQuantity(inventory.getReservedQuantity() + quantity);
        return this.inventoryRepository.save(inventory);
    }

    @Transactional
    public Inventory releaseStock(Long productId, int quantity) {
        Inventory inventory = this.findByProductId(productId);
        int newReserved = Math.max(0, inventory.getReservedQuantity() - quantity);
        inventory.setReservedQuantity(newReserved);
        return this.inventoryRepository.save(inventory);
    }

    @Transactional
    public Inventory confirmStockDeduction(Long productId, int quantity) {
        Inventory inventory = this.findByProductId(productId);
        inventory.setQuantity(inventory.getQuantity() - quantity);
        inventory.setReservedQuantity(inventory.getReservedQuantity() - quantity);
        return this.inventoryRepository.save(inventory);
    }
}
