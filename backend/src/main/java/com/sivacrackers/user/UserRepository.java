package com.sivacrackers.user;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserRepository extends JpaRepository<User, Long> {

    Optional<User> findByEmailIgnoreCase(String email);

    Optional<User> findByPhone(String phone);

    boolean existsByEmailIgnoreCase(String email);

    boolean existsByPhone(String phone);

    /** Used by the one-time OWNER bootstrap (Phase 4.5). */
    boolean existsByRole(Role role);

    // ---------- Staff management (Phase 5) ----------

    List<User> findAllByRoleInOrderByRoleAscFullNameAsc(Collection<Role> roles);

    boolean existsByEmailIgnoreCaseAndIdNot(String email, Long id);

    boolean existsByPhoneAndIdNot(String phone, Long id);

    long countByRoleIn(Collection<Role> roles);

    long countByRoleInAndActiveTrue(Collection<Role> roles);
}
