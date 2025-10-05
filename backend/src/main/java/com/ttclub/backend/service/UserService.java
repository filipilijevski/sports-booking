package com.ttclub.backend.service;

import com.ttclub.backend.dto.*;
import com.ttclub.backend.mapper.UserMapper;
import com.ttclub.backend.model.AuthProvider;
import com.ttclub.backend.model.Role;
import com.ttclub.backend.model.RoleName;
import com.ttclub.backend.model.User;
import com.ttclub.backend.repository.RoleRepository;
import com.ttclub.backend.repository.UserRepository;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.criteria.*;
import jakarta.transaction.Transactional;
import org.springframework.data.domain.*;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

@Service
@Transactional
public class UserService {

    private final UserRepository users;
    private final RoleRepository roles;
    private final PasswordEncoder encoder;
    private final UserMapper mapper;

    @PersistenceContext
    private EntityManager em;

    public UserService(UserRepository users,
                       RoleRepository roles,
                       PasswordEncoder encoder,
                       UserMapper mapper) {
        this.users = users;
        this.roles = roles;
        this.encoder = encoder;
        this.mapper = mapper;
    }

    /* Self-Service */

    public UserDto updateOwn(Long userId, UpdateProfileDto dto) {
        User u = users.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));

        if (dto.firstName() != null) u.setFirstName(dto.firstName().trim());
        if (dto.lastName()  != null) u.setLastName(dto.lastName().trim());
        if (dto.email() != null && !dto.email().equalsIgnoreCase(u.getEmail())) {
            if (u.getProvider() != AuthProvider.LOCAL) {
                throw new IllegalArgumentException("Email is managed by OAuth2 provider and cannot be changed.");
            }
            String newEmail = normalize(dto.email());
            if (!StringUtils.hasText(newEmail)) throw new IllegalArgumentException("Email cannot be blank.");
            if (users.existsByEmailIgnoreCaseAndIdNot(newEmail, u.getId())) {
                throw new IllegalArgumentException("Email already in use.");
            }
            u.setEmail(newEmail);
        }

        return mapper.toDto(users.save(u));
    }

    public void changePasswordOwn(Long userId, ChangePasswordDto dto) {
        User u = users.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));

        if (u.getProvider() != AuthProvider.LOCAL) {
            throw new IllegalArgumentException("Password is managed by OAuth2 provider and cannot be changed.");
        }
        if (!dto.newPassword().equals(dto.confirmPassword())) {
            throw new IllegalArgumentException("Passwords do not match.");
        }
        if (dto.newPassword().length() < 8) {
            throw new IllegalArgumentException("Password must be at least 8 characters.");
        }

        u.setPasswordHash(encoder.encode(dto.newPassword()));
        // Clear any temporary password upon successful change
        u.setTempPasswordHash(null);
        u.setTempPasswordExpiresAt(null);

        users.save(u);
    }

    /* Admin - Search/Update/Reset Password/Create/Delete */

    @PreAuthorize("hasRole('ADMIN') or hasRole('OWNER')")
    public Page<AdminUserDto> adminSearch(String q,
                                          RoleName role,
                                          AuthProvider provider,
                                          int page,
                                          int size) {
        CriteriaBuilder cb = em.getCriteriaBuilder();

        CriteriaQuery<User> cq = cb.createQuery(User.class);
        Root<User> root = cq.from(User.class);

        List<Predicate> ps = new ArrayList<>();
        ps.add(cb.isNull(root.get("deletedAt"))); // exclude soft-deleted

        if (StringUtils.hasText(q)) {
            String like = "%" + q.toLowerCase() + "%";
            Predicate byEmail = cb.like(cb.lower(root.get("email")), like);
            Predicate byFirst = cb.like(cb.lower(root.get("firstName")), like);
            Predicate byLast  = cb.like(cb.lower(root.get("lastName")), like);
            ps.add(cb.or(byEmail, byFirst, byLast));
        }
        if (role != null) ps.add(cb.equal(root.get("role").get("name"), role));
        if (provider != null) ps.add(cb.equal(root.get("provider"), provider));

        cq.where(ps.toArray(Predicate[]::new)).orderBy(cb.desc(root.get("createdAt")));

        var query = em.createQuery(cq).setFirstResult(page * size).setMaxResults(size);
        List<User> rows = query.getResultList();

        CriteriaQuery<Long> countQ = cb.createQuery(Long.class);
        Root<User> cntRoot = countQ.from(User.class);
        List<Predicate> cps = new ArrayList<>();
        cps.add(cb.isNull(cntRoot.get("deletedAt")));
        if (StringUtils.hasText(q)) {
            String like = "%" + q.toLowerCase() + "%";
            Predicate byEmail = cb.like(cb.lower(cntRoot.get("email")), like);
            Predicate byFirst = cb.like(cb.lower(cntRoot.get("firstName")), like);
            Predicate byLast  = cb.like(cb.lower(cntRoot.get("lastName")), like);
            cps.add(cb.or(byEmail, byFirst, byLast));
        }
        if (role != null) cps.add(cb.equal(cntRoot.get("role").get("name"), role));
        if (provider != null) cps.add(cb.equal(cntRoot.get("provider"), provider));
        countQ.select(cb.count(cntRoot)).where(cps.toArray(Predicate[]::new));
        long total = em.createQuery(countQ).getSingleResult();

        List<AdminUserDto> content = rows.stream().map(mapper::toAdminDto).toList();
        return new PageImpl<>(content, PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt")), total);
    }

    @PreAuthorize("hasRole('ADMIN') or hasRole('OWNER')")
    public AdminUserDto adminUpdate(long targetUserId, AdminUpdateUserDto dto) {
        User u = users.findById(targetUserId)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));

        if (dto.firstName() != null) u.setFirstName(dto.firstName().trim());
        if (dto.lastName()  != null) u.setLastName(dto.lastName().trim());

        if (dto.email() != null && !dto.email().equalsIgnoreCase(u.getEmail())) {
            if (u.getProvider() != AuthProvider.LOCAL) {
                throw new IllegalArgumentException("Email is managed by OAuth2 provider and cannot be changed.");
            }
            String newEmail = normalize(dto.email());
            if (!StringUtils.hasText(newEmail)) throw new IllegalArgumentException("Email cannot be blank.");
            if (users.existsByEmailIgnoreCaseAndIdNot(newEmail, u.getId())) {
                throw new IllegalArgumentException("Email already in use.");
            }
            u.setEmail(newEmail);
        }

        if (dto.role() != null) {
            Role r = roles.findByName(dto.role())
                    .orElseThrow(() -> new IllegalStateException("Role row missing: " + dto.role()));
            u.setRole(r);
        }

        return mapper.toAdminDto(users.save(u));
    }

    @PreAuthorize("hasRole('ADMIN') or hasRole('OWNER')")
    public void adminResetPassword(long targetUserId, String tempPassword) {
        User u = users.findById(targetUserId)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));

        if (u.getProvider() != AuthProvider.LOCAL) {
            throw new IllegalArgumentException("Password is managed by OAuth2 provider and cannot be changed.");
        }
        if (tempPassword == null || tempPassword.length() < 8) {
            throw new IllegalArgumentException("Temporary password must be at least 8 characters.");
        }
        u.setPasswordHash(encoder.encode(tempPassword));
        // clear temp fields if any
        u.setTempPasswordHash(null);
        u.setTempPasswordExpiresAt(null);
        users.save(u);
    }

    @PreAuthorize("hasRole('ADMIN') or hasRole('OWNER')")
    public AdminUserDto adminCreate(AdminCreateUserDto dto) {
        String email = normalize(dto.email());
        if (!StringUtils.hasText(email)) throw new IllegalArgumentException("Email cannot be blank.");
        if (users.existsByEmailIgnoreCase(email)) {
            throw new IllegalArgumentException("Email already in use.");
        }
        if (!StringUtils.hasText(dto.firstName()) || !StringUtils.hasText(dto.lastName())) {
            throw new IllegalArgumentException("First and last name are required.");
        }
        if (dto.password() == null || dto.password().length() < 8) {
            throw new IllegalArgumentException("Password must be at least 8 characters.");
        }
        RoleName rn = dto.role() == null ? RoleName.CLIENT : dto.role();
        Role r = roles.findByName(rn).orElseThrow(() -> new IllegalStateException("Role row missing: " + rn));

        User u = new User();
        u.setEmail(email);
        u.setPasswordHash(encoder.encode(dto.password()));
        u.setFirstName(dto.firstName().trim());
        u.setLastName(dto.lastName().trim());
        u.setRole(r);
        u.setProvider(AuthProvider.LOCAL);
        u.setMfaEnabled(false);
        users.save(u);

        return mapper.toAdminDto(u);
    }

    @PreAuthorize("hasRole('ADMIN') or hasRole('OWNER')")
    public void adminDelete(long currentUserId, long targetUserId) {
        if (currentUserId == targetUserId) {
            throw new IllegalArgumentException("You cannot delete your own account.");
        }
        User target = users.findById(targetUserId)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));

        if (target.getRole() != null && target.getRole().getName() == RoleName.OWNER) {
            // ensure another active OWNER exists
            List<User> owners = users.findByRole_Name(RoleName.OWNER);
            boolean hasAnother = owners.stream().anyMatch(u -> !u.getId().equals(targetUserId) && u.getDeletedAt() == null);
            if (!hasAnother) {
                throw new IllegalArgumentException("Cannot delete the only OWNER account.");
            }
        }

        target.setDeletedAt(Instant.now());
        users.save(target);
    }

    private static String normalize(String email) {
        if (email == null) return "";
        return email.trim().toLowerCase(Locale.ROOT);
    }
}
