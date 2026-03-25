package com.xyz.orders.service;

import com.xyz.orders.exception.ResourceNotFoundException;
import com.xyz.orders.model.Cart;
import com.xyz.orders.model.CartItem;
import com.xyz.orders.model.Product;
import com.xyz.orders.repository.CartItemRepository;
import com.xyz.orders.repository.CartRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CartService {

    private final CartRepository cartRepository;
    private final CartItemRepository cartItemRepository;
    private final ProductService productService;

    public Cart getOrCreateCart(String username) {
        return this.cartRepository.findByUsername(username)
                .orElseGet(() -> createCart(username));
    }

    public Cart findCartByUsername(String username) {
        return this.cartRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("Cart for user", username));
    }

    public List<CartItem> getCartItems(String username) {
        Cart cart = this.findCartByUsername(username);
        return this.cartItemRepository.findByCartId(cart.getId());
    }

    @Transactional
    public Cart createCart(String username) {
        Cart cart = Cart.builder()
                .username(username)
                .build();
        return this.cartRepository.save(cart);
    }

    @Transactional
    public CartItem addItem(String username, Long productId, int quantity) {
        Cart cart = this.getOrCreateCart(username);
        Product product =this.productService.findById(productId);

        return this.cartItemRepository.findByCartIdAndProductId(cart.getId(), productId)
                .map(existing -> {
                    existing.setQuantity(existing.getQuantity() + quantity);
                    return this.cartItemRepository.save(existing);
                })
                .orElseGet(() -> {
                    CartItem item = CartItem.builder()
                            .cart(cart)
                            .product(product)
                            .quantity(quantity)
                            .build();
                    return cartItemRepository.save(item);
                });
    }

    @Transactional
    public CartItem updateItemQuantity(String username, Long productId, int quantity) {
        Cart cart = this.findCartByUsername(username);
        CartItem item = this.cartItemRepository.findByCartIdAndProductId(cart.getId(), productId)
                .orElseThrow(() -> new ResourceNotFoundException("CartItem for product", productId));

        if (quantity <= 0) {
            this.cartItemRepository.delete(item);
            return item;
        }

        item.setQuantity(quantity);
        return this.cartItemRepository.save(item);
    }

    @Transactional
    public void removeItem(String username, Long productId) {
        Cart cart = this.findCartByUsername(username);
        CartItem item = this.cartItemRepository.findByCartIdAndProductId(cart.getId(), productId)
                .orElseThrow(() -> new ResourceNotFoundException("CartItem for product", productId));
        this.cartItemRepository.delete(item);
    }

    @Transactional
    public void clearCart(String username) {
        Cart cart = this.findCartByUsername(username);
        this.cartItemRepository.deleteByCartId(cart.getId());
    }
}
