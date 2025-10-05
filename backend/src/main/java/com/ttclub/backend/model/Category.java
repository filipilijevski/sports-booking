package com.ttclub.backend.model;

import jakarta.persistence.*;
import java.util.Objects;

/**
 * Simple lookup entity for product categories (e.g. Apparel, Balls ).
 * Existing table assumed to be named categories.
 */
@Entity
@Table(name = "categories")
public class Category {

    /* pk */
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 64)
    private String name;

    public Category() { }
    public Category(String name) { this.name = name; }

    public Long getId()                  { return id; }
    public void setId(Long id)           { this.id = id; }
    public String getName()              { return name; }
    public void setName(String name)     { this.name = name; }

    @Override public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Category c)) return false;
        return Objects.equals(id, c.id);
    }
    @Override public int hashCode() { return Objects.hash(id); }
    @Override public String toString() { return "Category[" + id + "," + name + "]"; }
}
