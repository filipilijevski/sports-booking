package com.ttclub.backend.repository;

import com.ttclub.backend.model.RoleName;
import com.ttclub.backend.model.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {

    Optional<User> findByEmail(String email);
    Optional<User> findByEmailIgnoreCase(String email);
    boolean existsByEmail(String email);
    boolean existsByEmailIgnoreCase(String email);

    /** For admin/email update - ensure uniqueness excluding the current user. */
    boolean existsByEmailAndIdNot(String email, Long id);
    boolean existsByEmailIgnoreCaseAndIdNot(String email, Long id);

    /**
     * Fetch users by their role's name.
     */
    List<User> findByRole_Name(RoleName roleName);

    /** Paged overload used by AdminUserSearchController to list only CLIENT users. */
    Page<User> findByRole_Name(RoleName roleName, Pageable pageable);

    /**
     * Return a single display name (first + last with safe null handling) for a user id.
     */
    @Query("""
           select concat(coalesce(u.firstName,''), 
                         case when u.firstName is not null and u.lastName is not null and u.lastName <> '' then ' ' else '' end, 
                         coalesce(u.lastName,'')) 
           from User u 
           where u.id = :id
           """)
    String findDisplayName(@Param("id") Long id);

    /**
     * Bulk fetch display names for many user ids at once with closed projection.
     */
    @Query("""
           select u.id as id,
                  concat(coalesce(u.firstName,''), 
                        case when u.firstName is not null and u.lastName is not null and u.lastName <> '' then ' ' else '' end, 
                        coalesce(u.lastName,'')) as name
           from User u
           where u.id in :ids
           """)
    List<UserNameView> findNamesByIds(@Param("ids") Collection<Long> ids);

    interface UserNameView {
        Long getId();
        String getName();
    }

    /* Admin search helper */

    @Query("""
           select u
             from User u
            where lower(u.email) like concat('%', :q, '%')
               or lower(u.firstName) like concat('%', :q, '%')
               or lower(u.lastName)  like concat('%', :q, '%')
           """)
    Page<User> searchByNameOrEmail(@Param("q") String q, Pageable pageable);

    /* Name/email search constrained to a role (used by admin enroll UI) */
    @Query("""
           select u from User u
            where u.role.name = :role
              and ( lower(u.email) like concat('%', :q, '%')
                 or lower(u.firstName) like concat('%', :q, '%')
                 or lower(u.lastName)  like concat('%', :q, '%') )
           """)
    Page<User> searchByRoleAndNameOrEmail(@Param("role") RoleName role,
                                          @Param("q") String q,
                                          Pageable pageable);
}
