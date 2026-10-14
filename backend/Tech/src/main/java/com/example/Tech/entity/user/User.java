package com.example.Tech.entity.user;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

/**
 * User account. Users are soft-deleted (deleted_at + is_active = false) because orders and
 * other records reference them without ON DELETE CASCADE.
 */
@Entity
@Table(name = "users")
@Getter
@Setter
@NoArgsConstructor
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "user_id")
    private Long id;

    @Column(name = "email", length = 120, nullable = false, unique = true)
    private String email;

    @Column(name = "username", length = 50, nullable = false, unique = true)
    private String username;

    @Column(name = "password_hash", length = 255, nullable = false)
    private String passwordHash;

    @Column(name = "fullname", length = 120, nullable = false)
    private String fullname;

    @Column(name = "phone", length = 20)
    private String phone;

    @Column(name = "avatar_url", columnDefinition = "text")
    private String avatarUrl;

    @Column(name = "is_active")
    private Boolean active = true;

    /** Set when the account is hired with a temporary password; cleared by the first password change. */
    @Column(name = "must_change_password", nullable = false)
    private boolean mustChangePassword = false;

    /** Deadline for changing the temporary password; null = no deadline. */
    @Column(name = "password_expires_at")
    private LocalDateTime passwordExpiresAt;

    @Column(name = "last_login")
    private LocalDateTime lastLogin;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @Column(name = "deleted_at")
    private LocalDateTime deletedAt;

    /** The temporary password was not changed in time: the account cannot log in until it is reissued. */
    public boolean isTemporaryPasswordExpired(LocalDateTime now) {
        return mustChangePassword && passwordExpiresAt != null && now.isAfter(passwordExpiresAt);
    }

    /** An account can be used only when it is not soft-deleted and not deactivated (null counts as active). */
    public boolean isEnabled() {
        return deletedAt == null && !Boolean.FALSE.equals(active);
    }
}
