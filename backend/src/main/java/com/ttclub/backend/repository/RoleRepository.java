package com.ttclub.backend.repository;

import com.ttclub.backend.model.Role;
import com.ttclub.backend.model.RoleName;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface RoleRepository extends JpaRepository<Role, Long> {
    Optional<Role> findByName(RoleName name);
}
