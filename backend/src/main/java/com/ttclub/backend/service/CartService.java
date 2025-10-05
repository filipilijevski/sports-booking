/* src/main/java/com/ttclub/backend/service/CartService.java */
package com.ttclub.backend.service;

import com.ttclub.backend.dto.CartDto;
import com.ttclub.backend.mapper.CartMapper;
import com.ttclub.backend.model.*;
import com.ttclub.backend.repository.*;
import jakarta.persistence.*;
import jakarta.transaction.Transactional;
import org.springframework.stereotype.Service;

/**
 * Cart operations. Policy change: we DO NOT “reserve” inventory for PENDING_PAYMENT;
 * availability checks are against current product.inventoryQty only.
 */
@Service
@Transactional
public class CartService {

    private final CartRepository     cartRepo;
    private final CartItemRepository cartItemRepo;
    private final ProductRepository  productRepo;
    private final UserRepository     userRepo;
    private final CartMapper         mapper;

    @PersistenceContext
    private EntityManager em;

    public CartService(CartRepository cartRepo,
                       CartItemRepository cartItemRepo,
                       ProductRepository productRepo,
                       UserRepository userRepo,
                       CartMapper mapper) {
        this.cartRepo     = cartRepo;
        this.cartItemRepo = cartItemRepo;
        this.productRepo  = productRepo;
        this.userRepo     = userRepo;
        this.mapper       = mapper;
    }

    /** Lock the product row to keep concurrent edits consistent. */
    private Product lockProduct(Long productId) {
        return em.find(Product.class, productId, LockModeType.PESSIMISTIC_WRITE);
    }

    /** Fetch or create the user's cart (by email). */
    private Cart getOrCreateCart(String userEmail) {
        User user = userRepo.findByEmail(userEmail)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));
        return cartRepo.findByUserId(user.getId())
                .orElseGet(() -> cartRepo.save(new Cart(user)));
    }

    public CartDto addItem(String userEmail, Long productId, int qty) {
        Cart cart       = getOrCreateCart(userEmail);
        Product product = lockProduct(productId);

        int avail = Math.max(0, product.getInventoryQty());
        if (qty > avail) throw new IllegalArgumentException("Not enough stock");

        CartItem item = cart.getItems().stream()
                .filter(ci -> ci.getProduct().getId().equals(productId))
                .findFirst()
                .orElseGet(() -> {
                    CartItem ci = new CartItem();
                    ci.setProduct(product);
                    ci.setUnitPrice(product.getPrice());
                    ci.setQuantity(0);
                    cart.addItem(ci);
                    return ci;
                });

        int newQty = item.getQuantity() + qty;
        if (newQty > avail) throw new IllegalArgumentException("Not enough stock");

        item.setQuantity(newQty);
        return mapper.toDto(cartRepo.save(cart));
    }

    public CartDto viewCart(String userEmail) {
        return mapper.toDto(getOrCreateCart(userEmail));
    }

    public CartDto updateItem(String userEmail, Long cartItemId, int qty) {
        CartItem item = cartItemRepo.findById(cartItemId)
                .orElseThrow(() -> new IllegalArgumentException("Item not found"));

        if (!item.getCart().getUser().getEmail().equals(userEmail))
            throw new SecurityException("Not owner of cart");

        Product product = lockProduct(item.getProduct().getId());
        int avail       = Math.max(0, product.getInventoryQty());

        if (qty > avail) throw new IllegalArgumentException("Not enough stock");

        item.setQuantity(qty);
        return mapper.toDto(item.getCart());
    }

    public CartDto removeItem(String userEmail, Long cartItemId) {
        CartItem item = cartItemRepo.findById(cartItemId)
                .orElseThrow(() -> new IllegalArgumentException("Item not found"));
        Cart cart = item.getCart();

        if (!cart.getUser().getEmail().equals(userEmail))
            throw new SecurityException("Not owner of cart");

        cart.removeItem(item);
        cartItemRepo.delete(item);
        return mapper.toDto(cart);
    }
}
