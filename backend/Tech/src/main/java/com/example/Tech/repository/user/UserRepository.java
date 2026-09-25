package com.example.Tech.repository.user;

import com.example.Tech.entity.user.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.Optional;

/**
 * Email and username are stored normalized (trimmed, lower case), so callers must normalize
 * the value before looking it up. Lookups include soft-deleted users, like the UNIQUE constraints.
 */
public interface UserRepository extends JpaRepository<User, Long>, JpaSpecificationExecutor<User> {

    Optional<User> findByEmail(String email);

    Optional<User> findByUsername(String username);

    boolean existsByEmail(String email);

    boolean existsByUsername(String username);
}
