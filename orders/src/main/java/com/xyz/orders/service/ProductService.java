package com.xyz.orders.service;

import com.xyz.orders.exception.ResourceNotFoundException;
import com.xyz.orders.model.Product;
import com.xyz.orders.repository.ProductRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ProductService {

    private final ProductRepository productRepository;

    public List<Product> findAll() {
        return this.productRepository.findAll();
    }

    public Product findById(Long id) {
        return this.productRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Product", id));
    }

    public List<Product> searchByName(String name) {
        return this.productRepository.findByNameContainingIgnoreCase(name);
    }

    @Transactional
    public Product create(Product product) {
        return this.productRepository.save(product);
    }

    @Transactional
    public Product update(Long id, Product updated) {
        Product product = this.findById(id);
        product.setName(updated.getName());
        product.setDescription(updated.getDescription());
        product.setPrice(updated.getPrice());
        return this.productRepository.save(product);
    }

    @Transactional
    public void delete(Long id) {
        Product product = this.findById(id);
        this.productRepository.delete(product);
    }
}
