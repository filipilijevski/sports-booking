package com.ttclub.backend.config;

import com.ttclub.backend.model.Role;
import com.ttclub.backend.model.RoleName;
import com.ttclub.backend.repository.RoleRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
class RoleBootstrap {

    /** Inserts four default roles once if they are missing. */
    @Bean
    CommandLineRunner initDefaultRoles(RoleRepository roles) {
        return args -> {
            for (RoleName rn : RoleName.values()) {
                roles.findByName(rn).orElseGet(() -> roles.save(new Role(rn)));
            }
        };
    }
}
