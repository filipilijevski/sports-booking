package com.ttclub.backend.security;

import com.ttclub.backend.model.RoleName;
import org.springframework.security.access.prepost.PreAuthorize;

import java.lang.annotation.*;

/**
 * Meta‑annotation to guard service methods.
 * Example:  {@code @RequiresRole(OWNER)}.
 */
@Target({ElementType.METHOD, ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Documented
@PreAuthorize("hasRole(#role.name())")
public @interface RequiresRole {
    RoleName role();
}
