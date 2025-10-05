package com.ttclub.backend.model;

import jakarta.persistence.*;

@Entity
@Table(name = "roles")
public class Role {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, unique = true, length = 20)
    private RoleName name;

    protected Role() {}                // JPA
    public Role(RoleName name) { this.name = name; }

    public Long      getId()   { return id; }
    public void      setId(Long id) { this.id = id; }

    public RoleName  getName() { return name; }
    public void      setName(RoleName name) { this.name = name; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Role other)) return false;
        return id != null && id.equals(other.id);
    }
    @Override
    public int hashCode() { return id != null ? id.hashCode() : 0; }

    @Override
    public String toString() { return name.name(); }
}
