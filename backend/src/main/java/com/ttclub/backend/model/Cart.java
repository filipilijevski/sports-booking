package com.ttclub.backend.model;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

@Entity
@Table(name = "carts",
        uniqueConstraints = @UniqueConstraint(name = "uk_cart_user", columnNames = "user_id"))
public class Cart {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /* owner */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id")
    private User user;

    @OneToMany(mappedBy = "cart",
            cascade = CascadeType.ALL,
            orphanRemoval = true)
    private List<CartItem> items = new ArrayList<>();

    /* audit */
    @Column(nullable = false) private Instant createdAt = Instant.now();
    @Column(nullable = false) private Instant updatedAt = Instant.now();

    public Cart() { }
    public Cart(User user) { this.user = user; }

    public Long getId()            { return id; }
    public void setId(Long id)     { this.id = id; }

    public User getUser()          { return user; }
    public void setUser(User user) { this.user = user; }

    public List<CartItem> getItems()              { return items; }
    public void setItems(List<CartItem> items)    { this.items = items; }

    public Instant getCreatedAt()                { return createdAt; }
    public void setCreatedAt(Instant createdAt)  { this.createdAt = createdAt; }
    public Instant getUpdatedAt()                { return updatedAt; }
    public void setUpdatedAt(Instant updatedAt)  { this.updatedAt = updatedAt; }

    public void addItem(CartItem item) {
        items.add(item);
        item.setCart(this);
    }
    public void removeItem(CartItem item) {
        items.remove(item);
        item.setCart(null);
    }

    @Override public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Cart c)) return false;
        return Objects.equals(id, c.id);
    }
    @Override public int hashCode() { return Objects.hash(id); }
    @Override public String toString() { return "Cart[" + id + "]"; }
}
